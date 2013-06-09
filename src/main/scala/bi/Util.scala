package bi

import java.io.File
import java.io.FileFilter
import java.sql.DriverManager
import java.sql.Timestamp
import java.util.Date
import scala.Array.canBuildFrom
import scala.Array.fallbackCanBuildFrom
import scala.xml.Node
import scala.xml.NodeSeq.seqToNodeSeq
import scala.io.Source
import scala.xml._
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.PrimitiveTypeMode.int2ScalarInt
import org.squeryl.PrimitiveTypeMode.transaction
import org.squeryl.PrimitiveTypeMode.view2QueryAll
import org.squeryl.Session
import org.squeryl.SessionFactory
import org.squeryl.adapters.MySQLAdapter
import com.typesafe.scalalogging.slf4j.Logging
import bi.MySchema._
import org.joda.time.LocalDate
import org.squeryl.Table
import org.squeryl.KeyedEntity
import bi.Aspects._

object Util extends Logging {
    val prettyPrinter = new PrettyPrinter(80, 2)
  def pp(node: Node): String = {
    val sb = new StringBuilder()
    prettyPrinter.format(node, sb)
    sb.toString
  }

  val matrnrRegex = """a\d+""".r
  val dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")
  val df = DateTimeFormat.forPattern("yyyy-MM-dd")
  val sdf = DateTimeFormat.forPattern("MMyyyy")
  val dmyf = DateTimeFormat.forPattern("dd.MM.yyyy")
  val tf = DateTimeFormat.forPattern("HH:mm")

  implicit def optionSeqXmlNode2String(osxn: Option[Seq[Node]]): String = osxn.get.text
  implicit def optionSeqXmlNode2Int(osxn: Option[Seq[Node]]): Int = optionSeqXmlNode2String(osxn).toInt
  implicit def optionSeqXmlNode2Long(osxn: Option[Seq[Node]]): Long = optionSeqXmlNode2String(osxn).toLong
  implicit def optionSeqXmlNode2LocalDate(osxn: Option[Seq[Node]]): LocalDate = df.parseLocalDate(optionSeqXmlNode2String(osxn))
  implicit def optionSeqXmlNode2Timestamp(osxn: Option[Seq[Node]]): Timestamp = new Timestamp(dtf.parseLocalDateTime(optionSeqXmlNode2String(osxn)).toDate().getTime())

  implicit def localDateTime2Timestamp(ldt: LocalDateTime): Timestamp = new Timestamp(ldt.toDate().getTime())
  implicit def localDate2Timestamp(ld: LocalDate): java.sql.Date = new java.sql.Date(ld.toDate().getTime())
  val dataDir = new File("C:/tools/uni/pentaho/data")

  val allTables = Map(
    Abgabe -> Seq(tAbgabeTestUser, tAbgabeFeedback, tAbgabeUpload, tAbgabeTest, tAbgabeSubtask, tAbgabeTask, tAbgabeMitarbeit, tAbgabeGruppe, tAbgabe),
    Code -> Seq(tCodeUpload, tCodeTopic),
    Forum -> Seq(tPostingRead, tPosting, tForumsbereich, tForum),
    Registrierung -> Seq(tUserSlot, tUserGruppe, tSlot, tRegisterGruppe, tRegistrierungUser, tRegistrierung),
    Global -> Seq(tUser, tLva))

  val dirs = Map(
    Abgabe -> new File(dataDir, "Abgabe"),
    Code -> new File(dataDir, "Code"),
    Forum -> new File(dataDir, "Forum"),
    Registrierung -> new File(dataDir, "Register"))

  import org.squeryl.SessionFactory

  val dirFilter = new FileFilter {
    override def accept(file: File) = file.isDirectory()
  }

  val nonDirFilter = new FileFilter {
    override def accept(file: File) = !file.isDirectory()
  }

  def delete(aspects: Aspect*) {
    transaction {
      if (aspects.contains(Forum))
        tPosting.update(tPosting.toArray.map(_.copy(parent_id = None)))
      for {
        aspect <- aspects
        table <- allTables(aspect)
      } {
        logger.info("emptying table " + aspect + "/" + table.name)
        table.deleteWhere(_ => 1 === 1)
      }
    }
  }

}