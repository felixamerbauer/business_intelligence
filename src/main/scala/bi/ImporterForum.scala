package bi

import com.typesafe.scalalogging.slf4j.Logging
import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import java.io.File
import bi.Util._
import bi.Aspects.AForum
import bi.DBSchema.{tPosting,tUser,tForum,tForumsbereich,tPostingRead}
import scala.xml.XML
import scala.xml.Node
import java.io.FileFilter
import scala.io.Source
import bi.Implicits._

object ImporterForum extends Logging {
  def forum() {
    logger.info("forum")
    delete(AForum)

    lazy val userIds = tUser.toArray.map(_.id).toSet

    def readAndInsertPostings(forumsId: Long, forumsbereichId: Long, forumsbereichDbid: Long) {
      // recursive method
      def process(node: Node, parent_id: Option[Long] = None) {
        // Data of a single posting
        val user: String = node.attribute("user")
        val name: String = node.attribute("name")
        val id: Int = node.attribute("id")
        val date: Timestamp = node.attribute("date")
        val betreff = (node \ "subject").text
        val text = (node \ "text").text
        if (userIds.contains(user)) {
          val postingDb = {
            val postingToInsert = Posting(postingid = id, datum = date, betreff_laenge = betreff.length(), text_laenge = text.length(), forumsbereich_id = forumsbereichDbid, parent_id = parent_id, user_id = user)
            logger.info("Inserting " + postingToInsert)
            tPosting.insert(postingToInsert)
          }
          // Optional answers
          node \ "sub" \ "entry" foreach { e => process(e, Some(postingDb.id)) }
        } else {
          logger.error(s"Unknown user $user in forumsbereich $forumsbereichId in forum $forumsId")
        }
      }
      val file = new File(dirs(AForum), s"$forumsId/Data/$forumsbereichId.xml")
      if (file.exists()) {
        val data = XML.loadFile(file)
        data \ "entry" foreach { e =>  process(e)  }
      } else {
        logger.error(s"No postings for forumsId $forumsId forumsbereichId $forumsbereichId")
      }
    }


    def processReadProgress(forumsIds: Seq[Long]) {
      val postingsForumsbereich = (from(tPosting, tForumsbereich)((p, f) =>
        where(p.forumsbereich_id === f.id)
          select (p, f))).toArray
      forumsIds.map { forumsId =>
        val dir = new File(dirs(AForum), s"$forumsId/User")
        val personDirs = dir.listFiles().filter(_.isDirectory())

        for (personDir <- personDirs) yield {
          println("Reading from " + personDir.getCanonicalPath())
          val readProgressFilter = new FileFilter {
            override def accept(file: File) = if ("""\d+""".r.pattern.matcher(file.getName).matches()) true
            else {
              logger.error("Invalid read progress file " + file.getCanonicalPath())
              false
            }
          }
          for {
            readProgressFile <- personDir.listFiles(readProgressFilter)
            bereichsId = readProgressFile.getName().toInt
            userId = personDir.getName
            postingId <- Source.fromFile(readProgressFile).getLines.map(_.toInt)
          } {
            postingsForumsbereich.find(e => e._2.forum_id == forumsId && e._2.bereichsid == bereichsId && e._1.postingid == postingId) match {
              case Some((posting, _)) =>
                val tToInsert = PostingRead(posting_id = posting.id, user_id = userId)
                logger.info("Inserting " + tToInsert)
                tPostingRead.insert(tToInsert)
              case None =>
                logger.error(s"No read progress for postingId $postingId forumsId $forumsId and userId $userId in file ${readProgressFile.getCanonicalPath()}")
            }
          }
        }
      }
    }

    def getForumsbereiche(forumsId: Long): Seq[Forumsbereich] =
      XML.loadFile(new File(dirs(AForum), s"$forumsId/issues.xml")) \ "entry" map { e =>
        val id: Int = e.attribute("id")
        val name: String = e.attribute("what")
        val beschreibung: String = (e \ "text").text
        Forumsbereich(bereichsid = id, name = name, beschreibung = beschreibung, forum_id = forumsId)
      }

    def forums = {
      readInstances(new File(dirs(AForum), "descriptions.xml")).filter(_.id == 100).map { e =>
        val toInsert = new Forum(id = e.id, langtext = e.name, lva_dbid = e.lva.id)
        logger.info("Inserting " + toInsert)
        tForum.insert(toInsert)
      }
    }
    def forumsbereiche = {
        val toInsert = forums.flatMap(e => getForumsbereiche(e.id))
        logger.info("Inserting\n\t" + toInsert.mkString("-" * 30, "\n\t", "-" * 30))
        tForumsbereich.insert(toInsert)
        tForumsbereich.toArray
      }
    transaction {
      forumsbereiche.foreach { e =>
        readAndInsertPostings(forumsId = e.forum_id, forumsbereichId = e.bereichsid, forumsbereichDbid = e.id)
      }
      processReadProgress(forums.map(_.id))
    }
  }
}

