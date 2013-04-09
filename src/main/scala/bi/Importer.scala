package bi

import com.typesafe.scalalogging.slf4j.Logging
import java.io.File
import scala.xml.XML
import scala.xml.Node
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDateTime
import scala.io.Source
import java.io.FilenameFilter
import java.io.FileFilter
import java.nio.file.Files
import scala.io.Codec
import java.nio.file.StandardOpenOption
import java.nio.file.Paths._
import java.nio.file.Path

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
      def read(node: Node): Posting = {
        val user: String = node.attribute("user")
        // TODO use
        val name: String = node.attribute("name")
        val id: Int = node.attribute("id")
        val date: LocalDateTime = node.attribute("date")
        val betreff = (node \ "subject").text
        val text = (node \ "text").text
        val subs = node \ "sub" \ "entry" map { e => read(e) }
        // TODO set parent
        Posting(id, date, betreffLänge = betreff.length(), textLänge = text.length, subs, None)
      }
      val file = new File(forumDir, s"$forumsId/Data/$forumsbereichId.xml")
      if (file.exists()) {
        val data = XML.loadFile(file)
        data \ "entry" map { e => read(e) }
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

  def writeCsv(file: Path, writable: Seq[CsvWritable]) {
    val writer = Files.newBufferedWriter(file, Codec.UTF8.charSet, StandardOpenOption.CREATE)
    writer.write(writable.head.csvHeader + "\n")
    writable.foreach(e => writer.write(e.csv + "\n"))
    writer.flush()
    writer.close
  }
  def writeCsv(forums: Seq[Forum], persons: Seq[Person]) {
    writeCsv(new File(csvDir.toFile(), "forums.csv").toPath, forums)
    writeCsv(new File(csvDir.toFile(), "persons.csv").toPath, persons)
  }

  val (forums, persons) = readForum
  writeCsv(forums, persons)
}