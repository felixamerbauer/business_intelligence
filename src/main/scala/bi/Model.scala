package bi

import org.joda.time.LocalDate
import org.joda.time.LocalDateTime

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
  personType: PersonType)

case class Lva(name: String, semester: Semester)

case class Semester(val id: Int) extends AnyVal

// Forum
case class Forum(id: Int, lva: Lva, bereiche: Seq[Forumsbereich])

case class Forumsbereich(id: Int, name: String, postings: Seq[Posting])

case class Posting(id: Int, datum: LocalDateTime, sub: Seq[Posting])

