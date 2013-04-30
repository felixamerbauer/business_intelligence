package bi

import java.io.File
import java.io.FileFilter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths.get
import java.nio.file.StandardOpenOption

import scala.Array.canBuildFrom
import scala.io.Codec
import scala.io.Source
import scala.xml.Node
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.XML

import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

import com.typesafe.scalalogging.slf4j.Logging
import scala.language.implicitConversions

object Importer extends App with Logging {
  val dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")

  implicit def optionSeqXmlNode2String(osxn: Option[Seq[Node]]): String = osxn.get.text
  implicit def optionSeqXmlNode2Int(osxn: Option[Seq[Node]]): Int = optionSeqXmlNode2String(osxn).toInt
  implicit def optionSeqXmlNode2LocalDateTime(osxn: Option[Seq[Node]]): LocalDateTime = dtf.parseLocalDateTime(optionSeqXmlNode2String(osxn))

  val dataDir = new File("C:/tools/uni/pentaho/data")
  val csvDir = get("csv")
  logger.info("start")
  readForum

  def readForum(): (Seq[Forum], Seq[Person]) = {
    val forumDir = new File(dataDir, "forum")

    def getPostings(forumsId: Int, forumsbereichId: Int): Seq[Posting] = {
      // recursive method
      def read(node: Node): Posting = {
        // Data of a single posting
        val user: String = node.attribute("user")
        val name: String = node.attribute("name")
        val id: Int = node.attribute("id")
        val date: LocalDateTime = node.attribute("date")
        val betreff = (node \ "subject").text
        val text = (node \ "text").text
        // Optional answers
        val children = node \ "sub" \ "entry" map { e => read(e) }
        // Build posting
        Posting(id, date, betreffLaenge = betreff.length(), textLaenge = text.length, children, None)
      }
      val file = new File(forumDir, s"$forumsId/Data/$forumsbereichId.xml")
      if (file.exists()) {
        val data = XML.loadFile(file)
        data \ "entry" map { e => read(e) }
      } else {
        logger.error(s"No postings for forumsId $forumsId forumsbereichId $forumsbereichId")
        Seq()
      }
    }

    def getForumsbereiche(forumsId: Int): Seq[Forumsbereich] =
      XML.loadFile(new File(forumDir, s"$forumsId/issues.xml")) \ "entry" map { e =>
        val id: Int = e.attribute("id")
        val name: String = e.attribute("what")
        Forumsbereich(id, name, getPostings(forumsId, id))
      }

    def getForums: Seq[Forum] = {
      val descriptions = XML.loadFile(new File(forumDir, "descriptions.xml"))
      val pattern = """KURS(\d\d)/?u?e?/se(\d\d)""".r
      descriptions \ "instance" map { e =>
        val id: Int = e.attribute("id")
        val pattern(lva, semester) = pattern.findFirstMatchIn(e.text).get
        Forum(id, Lva(lva, Semester(semester.toInt)), getForumsbereiche(id))
      }
    }

    def getPersons(forumsIds: Seq[Int]): Seq[(Int, Seq[Person])] = {
      def personsForum(forumsId: Int): Seq[Person] = {
        val data = XML.loadFile(new File(forumDir, s"$forumsId/persons.xml"))
        data \ "person" map { e =>
          val id: String = e.attribute("id")
          val name: String = e.attribute("name")
          val email: String = e.attribute("email")
          Person(id = id,
            name = name,
            email = email,
            geschlecht = None,
            dateOfBirth = None,
            // differntiate all types
            personType = { if (id.startsWith("a")) Student else Lecturer })
        }
      }
      forumsIds.map(e => (e, personsForum(e)))
    }

    def getReadProgress(forumsIds: Seq[Int]) = {
      forumsIds.map { forumsId =>
        val dir = new File(forumDir, s"$forumsId/User")
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
          val readProgress = for (readProgressFile <- personDir.listFiles(readProgressFilter)) yield {
            (readProgressFile.getName, Source.fromFile(readProgressFile).getLines.map(_.toInt))
          }
          (personDir.getName(), readProgress)
        }
      }
    }

    val forums = getForums
    val forumIds = forums.map(_.id)

    val readProgress = getReadProgress(forumIds)

    val personsPerForum = getPersons(forumIds)

    val personsWithDupes = personsPerForum.map(_._2).flatten.sortBy(_.id)

    (forums, /* TODO readProgress*/ personsWithDupes)
  }
  def code() {
    logger.info("code")
    val dirFilter = new FileFilter {
      override def accept(file: File) = file.isDirectory()
    }
    val codeDir = new File(dataDir, "code")
    def getInstances: Seq[Int] = codeDir.listFiles(dirFilter).map(_.getName().toInt)

    def getUploads(instance: Int): Seq[(String, LocalDateTime)] = {
      // e.g. C:/tools/uni/pentaho/data/Code/1/Data/1/a0127319
      new File(codeDir, s"$instance/Data/1").listFiles(dirFilter).map { matrnrDir =>
        val currentFile = matrnrDir.listFiles(dirFilter).sortBy(_.lastModified()).last
        (matrnrDir.getName(),LocalDateTime.now())
      }
    }

  }
  def forum() {
    logger.info("forum")

  }
  args match {
    case Array("code") => code()
    case Array("forum") => forum()
    case _ => println("Invalid parameters")
  }
  val (forums, persons) = readForum
}