package bi

import org.joda.time.LocalDate
import org.joda.time.LocalDateTime

trait CsvWritable {
  def csv: String
  def csvHeader: String
}

// helpers
sealed abstract class Sex
case object Male extends Sex
case object Female extends Sex

sealed abstract class PersonType
case object Student extends PersonType
case object Tutor extends PersonType
case object Lecturer extends PersonType

// General
case class Person(
  id: String,
  name: String,
  email: String,
  geschlecht: Option[Sex],
  dateOfBirth: Option[LocalDate],
  personType: PersonType) extends CsvWritable {
  override lazy val csv = s"$id$s$name$s$email$s$geschlecht$s$dateOfBirth$s$personType"
  override def csvHeader = Person.header
}
object Person {
  private val header = s"id${s}name${s}email${s}geschlecht${s}dateOfBirth${s}personType"
}

case class Lva(name: String, semester: Semester) extends CsvWritable {
  override lazy val csv = s"$name"
  override def csvHeader = Lva.header
}
object Lva {
  val header = "name"
}

case class Semester(val id: Int) extends CsvWritable {
  override lazy val csv = s"$id"
  override def csvHeader = Semester.header
}
object Semester {
  private val header = "id"
}

// Forum
case class Forum(id: Int, lva: Lva, bereiche: Seq[Forumsbereich]) extends CsvWritable {
  override lazy val csv = s"$id$s${lva.name}"
  override def csvHeader = Forum.header
}
object Forum {
  private val header = s"id${s}lva.name"
}

case class Forumsbereich(id: Int, name: String, postings: Seq[Posting]) extends CsvWritable {
  override lazy val csv = s"$id$s$name"
  override def csvHeader = Forumsbereich.header
}
object Forumsbereich {
  private val header = s"id${s}name"
}

case class Posting(id: Int, datum: LocalDateTime, betreffLänge: Int, textLänge: Int, sub: Seq[Posting], parent: Option[Posting]) extends CsvWritable {
  override lazy val csv = s"$id$s$datum$s$betreffLänge$s$textLänge$s${parent.map(_.id)}"
  override def csvHeader = Posting.header
}
object Posting {
  private val header = s"id${s}datum${s}betreffLänge${s}textLänge${s}parent"
}

