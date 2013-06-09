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
import scala.xml.PrettyPrinter
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
import bi.DBSchema._
import org.joda.time.LocalDate
import org.squeryl.Table
import org.squeryl.KeyedEntity
import bi.Aspects._
import scala.xml.XML

object Implicits extends Logging {
  import scala.language.implicitConversions
  private val dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")
  private val df = DateTimeFormat.forPattern("yyyy-MM-dd")

  implicit def optionSeqXmlNode2String(osxn: Option[Seq[Node]]): String = osxn.get.text
  implicit def optionSeqXmlNode2Int(osxn: Option[Seq[Node]]): Int = optionSeqXmlNode2String(osxn).toInt
  implicit def optionSeqXmlNode2Long(osxn: Option[Seq[Node]]): Long = optionSeqXmlNode2String(osxn).toLong
  implicit def optionSeqXmlNode2Boolean(osxn: Option[Seq[Node]]): Boolean = optionSeqXmlNode2String(osxn).toBoolean
  implicit def optionSeqXmlNode2LocalDate(osxn: Option[Seq[Node]]): LocalDate = df.parseLocalDate(optionSeqXmlNode2String(osxn))
  implicit def optionSeqXmlNode2Timestamp(osxn: Option[Seq[Node]]): Timestamp = new Timestamp(dtf.parseLocalDateTime(optionSeqXmlNode2String(osxn)).toDate().getTime())

  implicit def localDateTime2Timestamp(ldt: LocalDateTime): Timestamp = new Timestamp(ldt.toDate().getTime())
  implicit def localDate2Timestamp(ld: LocalDate): java.sql.Date = new java.sql.Date(ld.toDate().getTime())
}
