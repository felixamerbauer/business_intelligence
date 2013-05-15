package bi

import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import java.sql.Timestamp
import org.squeryl.KeyedEntity
import org.squeryl.PersistenceStatus
import java.sql.Date

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

case class Lva(id: Long, name: String, semester: String) extends KeyedEntity[Long]

// Forum
case class Forum(id: Long, langtext: String, lva_dbid: Long) extends KeyedEntity[Long]

case class Forumsbereich(id: Long, bereichsid: Long, name: String, beschreibung: String, forum_id: Long) extends KeyedEntity[Long]

case class Posting(id: Long, postingid: Long, datum: Timestamp, betreff_laenge: Int, text_laenge: Int, forumsbereich_id: Long, parent_id: Option[Long]) extends KeyedEntity[Long]

// Code
case class CodeTopic(id: Long, name: String, lva_dbid: Long) extends KeyedEntity[Long]

case class CodeUpload(id: Long, zeitstempel: Timestamp, user_id: String, code_topic_id: Int) extends KeyedEntity[Long]

// Abgabe
case class Abgabe(id: Long, name: String, lva_dbid: Long) extends KeyedEntity[Long]

case class Task(id: Long, task_id: Int, kurzbezeichnung: String, beschreibung: String, abgabe_id: Long, beginn: Timestamp, ende: Timestamp, presence: java.util.Date, lva_gruppe_id: Long) extends KeyedEntity[Long]

case class Subtask(id: Long, subtask_id: Int, text: String, task_id: Long) extends KeyedEntity[Long]

case class Mitarbeit(id: Long, plusminus: Int, task_id: Long, user_id: String) extends KeyedEntity[Long]

case class Feedback(id: Long, kommentar: String, autor_id: String, student_id: String, subtask_id: Long) extends KeyedEntity[Long]

object MySchema extends Schema {
  val tLva = table[Lva]("lva")
  on(tLva)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tCodeTopic = table[CodeTopic]("code_topic")
  on(tCodeTopic)(g => declare(
    g.id is (primaryKey)))

  val tCodeUpload = table[CodeUpload]("code_upload")
  on(tCodeUpload)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tForum = table[Forum]("forum")
  on(tForum)(g => declare(
    g.id is (primaryKey)))

  val tForumsbereich = table[Forumsbereich]("forumsbereich")
  on(tForumsbereich)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tPosting = table[Posting]("posting")
  on(tPosting)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tAbgabe = table[Abgabe]("abgabe")
  on(tAbgabe)(g => declare(
    g.id is (primaryKey)))

  val tTask = table[Task]("task")
  on(tTask)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tSubtask = table[Subtask]("subtask")
  on(tSubtask)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tMitarbeit = table[Mitarbeit]("mitarbeit")
  on(tMitarbeit)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tFeedback = table[Feedback]("abgabe")
  on(tFeedback)(g => declare(
    g.id is (primaryKey)))
}
