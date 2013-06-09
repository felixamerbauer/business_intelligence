package bi

import org.squeryl.PrimitiveTypeMode.long2ScalarLong
import org.squeryl.PrimitiveTypeMode.string2ScalarString
import org.squeryl.Schema

object DBSchema extends Schema {
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
