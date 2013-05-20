package bi

import java.sql.Timestamp
import java.util.Date

import org.joda.time.LocalDate
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema

// General
case class User(id: String, email: Option[String], name: Option[String], admin: Option[Boolean], geschlecht: Option[String]) extends KeyedEntity[String]

case class Lva(id: Long, name: String, semester: String) extends KeyedEntity[Long]

// Registrierung
case class Registrierung(id: Long, name: String, lva_dbid: Long) extends KeyedEntity[Long]

case class RegisterGruppe(id: Long = -1, gruppe_id: Long, title: String, registrierung_id: Long, beginn: Option[Timestamp], ende: Option[Timestamp]) extends KeyedEntity[Long]

case class Slot(id: Long = -1, slot_id: Int, unit: Int, start: Option[String], ende: Option[String], register_gruppe_id: Long) extends KeyedEntity[Long]

case class UserGruppe(id: Long = -1, user_id: String, register_gruppe_id: Long) extends KeyedEntity[Long]

case class UserSlot(id: Long = -1, user_id: String, slot_id: Long) extends KeyedEntity[Long]

// helper table
case class RegistrierungUser(id: Long = -1, registrierung_id: Long, user_id: String, name: String, lva_id: Long) extends KeyedEntity[Long]

// Forum
case class Forum(id: Long, langtext: String, lva_dbid: Long) extends KeyedEntity[Long]

case class Forumsbereich(id: Long = -1, bereichsid: Long, name: String, beschreibung: String, forum_id: Long) extends KeyedEntity[Long]

case class Posting(id: Long = -1, postingid: Long, datum: Timestamp, betreff_laenge: Int, text_laenge: Int, forumsbereich_id: Long, parent_id: Option[Long], user_id: String) extends KeyedEntity[Long]

case class PostingRead(id: Long = -1, posting_id: Long, user_id: String) extends KeyedEntity[Long]

// Code
case class CodeTopic(id: Long, name: String, lva_dbid: Long) extends KeyedEntity[Long]

case class CodeUpload(id: Long, zeitstempel: Timestamp, user_id: String, code_topic_id: Int) extends KeyedEntity[Long]

// Abgabe
case class Abgabe(id: Long, name: String, lva_id: Long) extends KeyedEntity[Long]

case class AbgabeFeedback(id: Long = -1, autor_id: String, student_id: String, subtask_id: Long) extends KeyedEntity[Long]

case class AbgabeGruppe(id: Long = -1, gruppe_id: Int, abgabe_id: Long) extends KeyedEntity[Long]

case class AbgabeMitarbeit(id: Long = -1, plusminus: Int, abgabe_id: Long, user_id: String) extends KeyedEntity[Long]

case class AbgabeTask(id: Long = -1, task_id: Int, kurzbezeichnung: String, beschreibung: String, abgabe_id: Long, beginn: Option[Timestamp], ende: Option[Timestamp], presence: Option[Date]) extends KeyedEntity[Long]

case class AbgabeSubtask(id: Long = -1, subtask_id: Int, text: String, task_id: Long) extends KeyedEntity[Long] {
  override def toString = s"id $id, subtask_id $subtask_id, task_id $task_id"
}

case class AbgabeTest(id: Long = -1, abgabe_gruppe_id: Long, test_id: Long, beschreibung: String) extends KeyedEntity[Long]

case class AbgabeTestUser(id: Long = -1, abgabe_test_id: Long, user_id: String, result: Float) extends KeyedEntity[Long]

case class AbgabeUpload(id: Long = -1, zeitpunkt: Timestamp, subtask_id: Long, user_id: String) extends KeyedEntity[Long]

object MySchema extends Schema {
  val tUser = table[User]("user")
  on(tUser)(g => declare(
    g.id is (primaryKey)))

  val tLva = table[Lva]("lva")
  on(tLva)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  // Code
  val tCodeTopic = table[CodeTopic]("code_topic")
  on(tCodeTopic)(g => declare(
    g.id is (primaryKey)))

  val tCodeUpload = table[CodeUpload]("code_upload")
  on(tCodeUpload)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  // Forum
  val tForum = table[Forum]("forum")
  on(tForum)(g => declare(
    g.id is (primaryKey)))

  val tForumsbereich = table[Forumsbereich]("forumsbereich")
  on(tForumsbereich)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tPosting = table[Posting]("posting")
  on(tPosting)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tPostingRead = table[PostingRead]("posting_read")
  on(tPostingRead)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  // Abgabe
  val tAbgabe = table[Abgabe]("abgabe")
  on(tAbgabe)(g => declare(
    g.id is (primaryKey)))

  val tAbgabeFeedback = table[AbgabeFeedback]("abgabe_feedback")
  on(tAbgabeFeedback)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tAbgabeGruppe = table[AbgabeGruppe]("abgabe_gruppe")
  on(tAbgabeGruppe)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tAbgabeMitarbeit = table[AbgabeMitarbeit]("abgabe_mitarbeit")
  on(tAbgabeMitarbeit)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tAbgabeTask = table[AbgabeTask]("abgabe_task")
  on(tAbgabeTask)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tAbgabeSubtask = table[AbgabeSubtask]("abgabe_subtask")
  on(tAbgabeSubtask)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tAbgabeTest = table[AbgabeTest]("abgabe_test")
  on(tAbgabeTest)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tAbgabeTestUser = table[AbgabeTestUser]("abgabe_test_user")
  on(tAbgabeTestUser)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tAbgabeUpload = table[AbgabeUpload]("abgabe_upload")
  on(tAbgabeUpload)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  // Registrierung
  val tRegistrierung = table[Registrierung]("registrierung")
  on(tRegistrierung)(g => declare(
    g.id is (primaryKey)))

  val tRegisterGruppe = table[RegisterGruppe]("register_gruppe")
  on(tRegisterGruppe)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tSlot = table[Slot]("slot")
  on(tSlot)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tUserGruppe = table[UserGruppe]("user_gruppe")
  on(tUserGruppe)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  val tUserSlot = table[UserSlot]("user_slot")
  on(tUserSlot)(g => declare(
    g.id is (primaryKey, autoIncremented)))

  // helper
  val tRegistrierungUser = table[RegistrierungUser]("registrierung_user")
  on(tRegistrierungUser)(g => declare(
    g.id is (primaryKey, autoIncremented)))

}
