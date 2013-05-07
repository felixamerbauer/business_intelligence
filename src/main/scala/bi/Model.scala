package bi

import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import java.sql.Timestamp
import org.squeryl.KeyedEntity
import org.squeryl.PersistenceStatus

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

case class Semester(val id: Int)

// Forum
case class Forum(id: Long, langtext: String, lva_dbid: Long) extends KeyedEntity[Long]

/*
CREATE TABLE `forumsbereich` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bereichsid` int(11) NOT NULL,
  `name` varchar(64) DEFAULT NULL,
  `beschreibung` varchar(256) DEFAULT NULL,
  `forum_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `forumsbereich_unique` (`bereichsid`,`forum_id`),
  KEY `forumsbereich_forum_idx` (`forum_id`),
  CONSTRAINT `forumsbereich_forum` FOREIGN KEY (`forum_id`) REFERENCES `forum` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1
*/

case class Forumsbereich(id: Long, bereichsid: Long, name: String, beschreibung: String, forum_id: Long) extends KeyedEntity[Long]

case class Posting(id: Long, postingid: Long, datum: Timestamp, betreff_laenge: Int, text_laenge: Int, forumsbereich_id: Long, parent_id: Option[Long]) extends KeyedEntity[Long]

case class CodeTopic(id: Long, name: String, lva_dbid: Long) extends KeyedEntity[Long]

case class CodeUpload(id: Long, zeitstempel: Timestamp, user_id: String, code_topic_id: Int) extends KeyedEntity[Long]

case class Abgabe(id: Long, name: String, lva_dbid: Long) extends KeyedEntity[Long]

case class Task(id: Long, kurzbezeichnung: String, beschreibung: String, abgabe_id: Int, beginn: Timestamp, ende: Timestamp, presence: Timestamp, lva_gruppe_id: Long) extends KeyedEntity[Long]

case class Subtask(id: Long, text: String, task_id: Long) extends KeyedEntity[Long]

case class Feedback( /*subtask_*/ id: Long, kommentar: String, autor_id: String, student_id: String) extends KeyedEntity[Long]

object MySchema extends Schema {
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

  val tTask = table[Task]("abgabe")
  on(tTask)(g => declare(
    g.id is (primaryKey)))

  val tSubtask = table[Subtask]("subtask")
  on(tSubtask)(g => declare(
    g.id is (primaryKey)))

  val tFeedback = table[Feedback]("abgabe")
  on(tFeedback)(g => declare(
    g.id is (primaryKey)))
}
