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
import scala.xml._
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.squeryl.PrimitiveTypeMode.__thisDsl
import org.squeryl.PrimitiveTypeMode.int2ScalarInt
import org.squeryl.PrimitiveTypeMode.transaction
import org.squeryl.PrimitiveTypeMode.view2QueryAll
import org.squeryl.Session
import org.squeryl.SessionFactory
import org.squeryl.adapters.MySQLAdapter
import com.typesafe.scalalogging.slf4j.Logging
import bi.MySchema._
import org.joda.time.LocalDate

/**
 * TODO
 * C:/tools/uni/pentaho/data/Code/5/Data/1/ref301
 */

object Importer extends App with Logging {
  import language.implicitConversions
  import language.postfixOps
  val prettyPrinter = new PrettyPrinter(80, 2)
  def pp(node: Node): String = {
    val sb = new StringBuilder()
    prettyPrinter.format(node, sb)
    sb.toString
  }

  val matrnrRegex = """a\d+""".r
  val dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")
  val df = DateTimeFormat.forPattern("yyyy-MM-dd")
  val dmyf = DateTimeFormat.forPattern("dd.MM.yyyy")

  implicit def optionSeqXmlNode2String(osxn: Option[Seq[Node]]): String = osxn.get.text
  implicit def optionSeqXmlNode2Int(osxn: Option[Seq[Node]]): Int = optionSeqXmlNode2String(osxn).toInt
  implicit def optionSeqXmlNode2Long(osxn: Option[Seq[Node]]): Long = optionSeqXmlNode2String(osxn).toLong
  implicit def optionSeqXmlNode2LocalDate(osxn: Option[Seq[Node]]): LocalDate = df.parseLocalDate(optionSeqXmlNode2String(osxn))
  implicit def optionSeqXmlNode2Timestamp(osxn: Option[Seq[Node]]): Timestamp = new Timestamp(dtf.parseLocalDateTime(optionSeqXmlNode2String(osxn)).toDate().getTime())

  implicit def localDateTime2Timestamp(ldt: LocalDateTime): Timestamp = new Timestamp(ldt.toDate().getTime())
  implicit def localDate2Timestamp(ld: LocalDate): java.sql.Date = new java.sql.Date(ld.toDate().getTime())
  val dataDir = new File("C:/tools/uni/pentaho/data")

  import org.squeryl.SessionFactory

  val dirFilter = new FileFilter {
    override def accept(file: File) = file.isDirectory()
  }
  val nonDirFilter = new FileFilter {
    override def accept(file: File) = !file.isDirectory()
  }

  def abgabe() {
    def processSubtask(node: Node, task: Task) {
      val id: Int = node.attribute("id")
      val text: String = (node \ "text").text
      val toInsert = Subtask(id = -1, subtask_id = id, text = text, task_id = task.id)
      logger.info("Inserting " + toInsert)
      val db = tSubtask.insert(toInsert)
    }
    // tasks.xml /group
    def processGroup(node: Node, abgabe: Abgabe) {
      val groupId: Int = node.attribute("id")

      // Test
      // <test id="1" desc="16.12.2008"/>
      node \ "task[@desc]" foreach { e =>
        val descString = e.attribute("desc")
        val desc = dmyf.parseLocalDate(descString)
      }

    }

    // tasks.xml /task
    def processTasks(node: Node, abgabe: Abgabe) {
      node \ "task" foreach { task =>
        // Data of a single posting
        val id: Int = task.attribute("id")
        val short: String = task.attribute("short")
        val long: String = task.attribute("long")
        // Aufgabenzuordung pro Gruppe
        val groupTask = node \ "group" \ "task" filter { e => val id: String = e.attribute("id"); id == "1" } head
        val from: Timestamp = groupTask.attribute("from")
        val to: Timestamp = groupTask.attribute("to")
        val presence: LocalDate = groupTask.attribute("presence")
        // TOOD lva_gruppe_id
        val toInsert = Task(id = -1, task_id = id, kurzbezeichnung = short, beschreibung = long, abgabe_id = abgabe.id, beginn = from, ende = to, presence = presence, lva_gruppe_id = -1)
        logger.info("Inserting " + toInsert)
        val db = tTask.insert(toInsert)
        // Optional subtasks
        node \ "subtask" foreach { e => processSubtask(e, db) }
      }
    }

    def processAssessment(node: Node, abgabe: Abgabe) {
      // user id
      val userId: String = node.attribute("id")
      // plus and minus
      val pluses: Seq[LocalDate] = node \ "plus" map { e =>
        val date: LocalDate = e.attribute("date")
        date
      }
      val minuses: Seq[LocalDate] = node \ "minus" map { e =>
        val date: LocalDate = e.attribute("date")
        date
      }
      val sum = pluses.size - minuses.size
      // TODO Assign to abgabe/uebungseinheit, task_id doesn't make sense
      val mitarbeit = Mitarbeit(id = -1, plusminus = sum, task_id = -1, user_id = userId)
      logger.info("Inserting " + mitarbeit)
      tMitarbeit.insert(mitarbeit)
      // results
      node \ "result" foreach { e =>
        val id: Int = e.attribute("id")
        val result: Double = e.text.toDouble
      }
      // TODO
    }

    def processFeedback(node: Node, abgabe: Abgabe) {
      node \ "person" foreach { person =>
        val studentId = person.attribute("id")
        val comment = person \ "comment" head
        val author: String = comment.attribute("author")
        val subtaskId: Int = comment.attribute("subtask")
        val task: String = comment.attribute("task")
        // check available
        val text = comment.text
        // TODO handle subtaskid related to taskId and abgabeId
        Feedback(id = -1, kommentar = text, autor_id = author, student_id = studentId, subtask_id = subtaskId)
      }
    }

    logger.info("abgabe")
    logger.info("emptying tables task, subtask, abgabe")
    transaction {
      Seq(tSubtask, tTask, tAbgabe).foreach(_.deleteWhere(_ => 1 === 1))
    }
    val abgabeDir = new File(dataDir, "Abgabe")

    transaction {
      val instances = readInstances(new File(abgabeDir, "descriptions.xml"))
      val abgaben = instances.map { e =>
        val toInsert = new Abgabe(id = e.id, name = e.name, lva_dbid = e.lva.id)
        logger.info("Inserting " + toInsert)
        tAbgabe.insert(toInsert)
      }
      abgaben foreach { abgabe =>
        def file(name: String) = new File(abgabeDir, s"${abgabe.id}/$name")
        // tasks
        checkAndReadFile(file("tasks.xml")) foreach { xml =>
          processTasks(xml, abgabe)
          xml \ "group" foreach { e => processGroup(e, abgabe) }
        }
        // assessments
        checkAndReadFile(file("assessments.xml")) foreach {
          _ \ "person" foreach { e => processAssessment(e, abgabe) }
        }
        // feedback
        checkAndReadFile(file("feedback.xml")) foreach {
          _ \ "person" foreach { e => processFeedback(e, abgabe) }
        }
      }

    }
  }

  logger.info("start")
  def code() {
    logger.info("code")
    val matrnrDirFilter = new FileFilter {
      override def accept(file: File) = file.isDirectory() && matrnrRegex.findFirstIn(file.getName()).isDefined
    }
    val codeDir = new File(dataDir, "code")
    def getInstances: Seq[Int] = codeDir.listFiles(dirFilter).map(_.getName().toInt)

    def getUploads(instance: Int): Seq[(String, LocalDateTime)] = {
      def getLatestCodeArtefact(dir: File): Option[File] = {
        val files = dir.listFiles(nonDirFilter)
        val dirs = dir.listFiles(dirFilter)
        (!files.isEmpty, !dirs.isEmpty) match {
          case (true, _) => Some(files.sortBy(_.lastModified()).last)
          case (false, true) =>
            val subdir = dirs.sortBy(_.lastModified()).last
            Some(subdir.listFiles(nonDirFilter).sortBy(_.lastModified).last)
          case _ =>
            logger.warn("No code artefacts in " + dir.getCanonicalPath())
            None
        }

      }
      // e.g. C:/tools/uni/pentaho/data/Code/1/Data/1/a0127319
      for {
        matrnrDir <- new File(codeDir, s"$instance/Data/1").listFiles(matrnrDirFilter)
        currentFile = getLatestCodeArtefact(matrnrDir)
        if (currentFile.isDefined)
      } yield {
        logger.debug("Checking " + matrnrDir.getCanonicalPath())

        (matrnrDir.getName(), LocalDateTime.fromDateFields(new Date(currentFile.get.lastModified())))
      }
    }
    val data = getInstances.map { e => (e, getUploads(e)) }
    // persist to db
    transaction {
      for {
        (instanceid, row) <- data
        (matrnr, lastModified) <- row
      } {
        val cd = CodeUpload(id = -1, zeitstempel = lastModified, user_id = matrnr, code_topic_id = instanceid)
        tCodeUpload.insert(cd)
      }
    }
    println(data)
  }
  def forum() {
    logger.info("forum")
    logger.info("emptying tables posting, forumsbereich")
    transaction {
      tPosting.deleteWhere(_ => 1 === 1)
      tForumsbereich.deleteWhere(_ => 1 === 1)
    }

    val forumDir = new File(dataDir, "forum")

    def readAndInsertPostings(forumsId: Long, forumsbereichId: Long, forumsbereichDbid: Long) {
      // recursive method
      def process(node: Node, parent_id: Option[Long] = None) {
        // Data of a single posting
        val user: String = node.attribute("user")
        val name: String = node.attribute("name")
        val id: Int = node.attribute("id")
        val date: Timestamp = node.attribute("date")
        val betreff = (node \ "subject").text
        val text = (node \ "text").text
        val postingDb = transaction {
          val postingToInsert = Posting(id = -1, postingid = id, datum = date, betreff_laenge = betreff.length(), text_laenge = text.length(), forumsbereich_id = forumsbereichDbid, parent_id = parent_id, user_id = user)
          logger.info("Inserting " + postingToInsert)
          tPosting.insert(postingToInsert)
        }
        // Optional answers
        node \ "sub" \ "entry" foreach { e => process(e, Some(postingDb.id)) }
      }
      val file = new File(forumDir, s"$forumsId/Data/$forumsbereichId.xml")
      if (file.exists()) {
        val data = XML.loadFile(file)
        data \ "entry" foreach { e => process(e) }
      } else {
        logger.error(s"No postings for forumsId $forumsId forumsbereichId $forumsbereichId")
      }
    }

    def getForumsbereiche(forumsId: Long): Seq[Forumsbereich] =
      XML.loadFile(new File(forumDir, s"$forumsId/issues.xml")) \ "entry" map { e =>
        val id: Int = e.attribute("id")
        val name: String = e.attribute("what")
        val beschreibung: String = (e \ "text").text
        Forumsbereich(id = -1, bereichsid = id, name = name, beschreibung = beschreibung, forum_id = forumsId)
      }

    val forums = transaction { tForum.toArray }
    val forumsbereiche =
      transaction {
        val toInsert = forums.flatMap(e => getForumsbereiche(e.id))
        logger.info("Inserting\n\t" + toInsert.mkString("-" * 30, "\n\t", "-" * 30))
        tForumsbereich.insert(toInsert)
        tForumsbereich.toArray
      }
    val posting = forumsbereiche.foreach(e => readAndInsertPostings(forumsId = e.forum_id, forumsbereichId = e.bereichsid, forumsbereichDbid = e.id))

  }

  def registrierung() {
    logger.info("registrierung")
    logger.info("emptying tables user_gruppe, slot, register_gruppe, registrierung")
    transaction {
      Seq(tUserGruppe, tSlot, tRegisterGruppe, tRegistrierung).foreach(_.deleteWhere(_ => 1 === 1))
    }
    val registerDir = new File(dataDir, "Register")

    transaction {
      val userIds = tUser.toArray.map(_.id).toSet
      readInstances(new File(registerDir, "descriptions.xml")) foreach { instance =>
        val toInsert = new Registrierung(id = instance.id, name = instance.name, lva_dbid = instance.lva.id)
        logger.info("Inserting " + toInsert)
        val registrierung = tRegistrierung.insert(toInsert)

        def file(name: String) = new File(registerDir, s"${registrierung.id}/$name")

        // <group id="3" title="Anmeldung ...">
        def processRegisterGruppe(node: Node) {
        }

        // register Gruppe
        val registerGruppen = (checkAndReadFile(file("custom.xml")) map { xml =>
          xml \ "group" map { e =>
            val toInsert = RegisterGruppe(gruppe_id = e.attribute("id"), title = e.attribute("title"), registrierung_id = registrierung.id)
            logger.info("Inserting " + toInsert)
            tRegisterGruppe.insert(toInsert)
          }
        }).getOrElse(Seq())
        // todo
        checkAndReadFile(file("registrations.xml")) foreach { xml =>
          xml \ "registration" foreach { e =>
            val group: Int = e.attribute("group")
            val userId: String = e.text
            val slot: Int = e.attribute("slot")
            // user_gruppe
            (userIds.contains(userId), registerGruppen.find(_.gruppe_id == group)) match {
              case (true, Some(registerGruppe)) =>
                val userGruppe = UserGruppe(user_id = userId, register_gruppe_id = registerGruppe.id)
                logger.info("Inserting " + userGruppe)
                tUserGruppe.insert(userGruppe)
              case (_, None) => logger.error(s"No register gruppe for group $group slot $slot userId $userId registrierung $registrierung")
              case (false, _) => logger.error(s"Unknow user for group $group slot $slot userId $userId registrierung $registrierung")
            }
          }
          // slot
          val slots = for {
            node <- xml \ "registration"
            group: Int = node.attribute("group")
            slot: Int = node.attribute("slot")
            registerGruppe = registerGruppen.find(_.gruppe_id == group)
            if (registerGruppe.isDefined)
          } yield (slot, registerGruppe.get)

          for ((slot, data) <- slots.groupBy(_._1)) {
            val registerGruppe = data.head._2
            val toInsert = Slot(slot_id = slot, start = None, ende = None, register_gruppe_id = registerGruppe.id)
            logger.info("Inserting " + toInsert)
            tSlot.insert(toInsert)
          }

          //          xml \ "group" foreach { e => processGroup(e, abgabe) }
        }
        //        // assessments
        //        checkAndReadFile(file("assessments.xml")) foreach {
        //          _ \ "person" foreach { e => processAssessment(e, abgabe) }
        //        }
        //        // feedback
        //        checkAndReadFile(file("feedback.xml")) foreach {
        //          _ \ "person" foreach { e => processFeedback(e, abgabe) }
        //        }
      }

    }

  }
  // Helper Methods
  def checkAndReadFile(file: File): Option[Elem] =
    if (file.exists())
      Some(XML.loadFile(file))
    else {
      logger.error("Missing file " + file.getCanonicalPath())
      None
    }

  def initDb {
    Class.forName("com.mysql.jdbc.Driver");
    SessionFactory.concreteFactory = Some(() =>
      Session.create(
        DriverManager.getConnection("jdbc:mysql://localhost/uni_business_intelligence?user=bi&password=bi"),
        new MySQLAdapter))
  }

  case class Instance(id: Int, name: String, lva: Lva)

  def readInstances(file: File): Seq[Instance] = {
    val lvas = tLva.toArray
    val pattern = """(?s)http://[^/]+/[^/]+/([^/]+)/.*(se\d\d)[^:]*:(.*)""".r
    XML.loadFile(file) \ "instance" map { node =>
      val id: Int = node.attribute("id")
      val url: String = node.text
      println("Checking " + url)
      val pattern(lvaName, lvaSemester, beschreibung) = url
      Instance(id = id, name = beschreibung, lva = lvas.find(e => e.name == lvaName && e.semester == lvaSemester).get)
    }
  }

  // Program start
  initDb
  args match {
    case Array("registrierung") => registrierung()
    case Array("code") => code()
    case Array("forum") => forum()
    case Array("abgabe") => abgabe()
    case _ => println("Invalid parameters")
  }
  //  val (forums, persons) = readForum
}


    //    def getPersons(forumsIds: Seq[Int]): Seq[(Int, Seq[Person])] = {
    //      def personsForum(forumsId: Int): Seq[Person] = {
    //        val data = XML.loadFile(new File(forumDir, s"$forumsId/persons.xml"))
    //        data \ "person" map { e =>
    //          val id: String = e.attribute("id")
    //          val name: String = e.attribute("name")
    //          val email: String = e.attribute("email")
    //          Person(id = id,
    //            name = name,
    //            email = email,
    //            geschlecht = None,
    //            dateOfBirth = None,
    //            // differntiate all types
    //            personType = { if (id.startsWith("a")) Student else Lecturer })
    //        }
    //      }
    //      forumsIds.map(e => (e, personsForum(e)))
    //    }
    //
    //    def getReadProgress(forumsIds: Seq[Int]) = {
    //      forumsIds.map { forumsId =>
    //        val dir = new File(forumDir, s"$forumsId/User")
    //        val personDirs = dir.listFiles().filter(_.isDirectory())
    //
    //        for (personDir <- personDirs) yield {
    //          println("Reading from " + personDir.getCanonicalPath())
    //          val readProgressFilter = new FileFilter {
    //            override def accept(file: File) = if ("""\d+""".r.pattern.matcher(file.getName).matches()) true
    //            else {
    //              logger.error("Invalid read progress file " + file.getCanonicalPath())
    //              false
    //            }
    //          }
    //          val readProgress = for (readProgressFile <- personDir.listFiles(readProgressFilter)) yield {
    //            (readProgressFile.getName, Source.fromFile(readProgressFile).getLines.map(_.toInt))
    //          }
    //          (personDir.getName(), readProgress)
    //        }
    //      }
    //    }
    //    val forumIds = forums.map(_.id)

    //    val readProgress = getReadProgress(forumIds)
    //
    //    val personsPerForum = getPersons(forumIds)
    //
    //    val personsWithDupes = personsPerForum.map(_._2).flatten.sortBy(_.id)

    //    (forums, /* TODO readProgress*/ personsWithDupes)

