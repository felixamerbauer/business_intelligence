package bi

import com.typesafe.scalalogging.slf4j.Logging
import java.io.File
import scala.xml.XML
import scala.xml.Node
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDateTime

object Importer extends App with Logging {
  val dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")

  implicit def optionSeqXmlNode2String(osxn: Option[Seq[Node]]): String = osxn.get.text
  implicit def optionSeqXmlNode2Int(osxn: Option[Seq[Node]]): Int = optionSeqXmlNode2String(osxn).toInt
  implicit def optionSeqXmlNode2LocalDateTime(osxn: Option[Seq[Node]]): LocalDateTime = dtf.parseLocalDateTime(optionSeqXmlNode2String(osxn))

  val dataDir = new File("C:/tools/uni/pentaho/data")
  logger.info("start")
  readForum

  def readForum() {
    val forumDir = new File(dataDir, "forum")
    def getPostings(forumsId: Int, forumsbereichId: Int): Seq[Posting] = {
      def read(node: Node) = {
        val user: String = node.attribute("user")
        // TODO use
        val name: String = node.attribute("name")
        val id: Int = node.attribute("id")
        val date: LocalDateTime = node.attribute("date")
        // TODO use text, subject
        Posting(id, date)
      }
      val file = new File(forumDir, s"$forumsId/Data/$forumsbereichId.xml")
      if (file.exists()) {
        val data = XML.loadFile(file)
        data \ "entry" flatMap { e =>
          val followUps = e \ "sub" \ "entry" map { e =>
            read(e)
          }
          followUps :+ read(e)
        }
      } else {
        logger.error(s"No postings for forumsId $forumsId forumsbereichId $forumsbereichId")
        Seq()
      }
      //      println(data)
      //      ???
    }

    def getForumsbereiche(forumsId: Int): Seq[Forumsbereich] = {
      val issues = XML.loadFile(new File(forumDir, s"$forumsId/issues.xml"))
      issues \ "entry" map { e =>
        val id: Int = e.attribute("id")
        val name: String = e.attribute("what")
        Forumsbereich(id, name, getPostings(forumsId, id))
      }
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
    val forums = getForums
    forums foreach println
    //    .map  match {
    //      case (Some(id),lvaName,semester) => Forum(id,Lva(lvaName,semester))
    //    }
  }
}