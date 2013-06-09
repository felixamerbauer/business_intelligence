//package bi
//
//import java.io.File
//import java.io.FileFilter
//import java.sql.DriverManager
//import java.sql.Timestamp
//import java.util.Date
//import scala.Array.canBuildFrom
//import scala.Array.fallbackCanBuildFrom
//import scala.xml.Node
//import scala.xml.NodeSeq.seqToNodeSeq
//import scala.io.Source
//import scala.xml._
//import org.joda.time.LocalDateTime
//import org.joda.time.format.DateTimeFormat
//import org.squeryl.PrimitiveTypeMode._
//import org.squeryl.PrimitiveTypeMode.int2ScalarInt
//import org.squeryl.PrimitiveTypeMode.transaction
//import org.squeryl.PrimitiveTypeMode.view2QueryAll
//import org.squeryl.Session
//import org.squeryl.SessionFactory
//import org.squeryl.adapters.MySQLAdapter
//import com.typesafe.scalalogging.slf4j.Logging
//import bi.MySchema._
//import org.joda.time.LocalDate
//import org.squeryl.Table
//import org.squeryl.KeyedEntity
//import bi.Aspect._
//
//object Importer extends App with Logging {
//  import language.implicitConversions
//  import language.postfixOps
//
//  def global() {
//    def doMetaCsv {
//      val metaCsv = Source.fromFile(new File(dataDir, "meta.csv"))("Cp1252").mkString
//      val metaCsvRegex = """(?m)^([^;]+);([^;]+);([^;]+);"([^;]+)"$""".r
//      val geburtdatumRegx = """\d{6}""".r
//      val users = (for (metaCsvRegex(matrNr, geschlecht, geburtsdatum, name) <- metaCsvRegex.findAllIn(metaCsv)) yield {
//        val geburtsdatumDb = geburtdatumRegx.findFirstMatchIn(geburtsdatum).map { e =>
//          sdf.parseLocalDate(geburtsdatum).toDate()
//        }
//        User(id = matrNr, email = None, name = Some(name), admin = None, geschlecht = Some(geschlecht), geburtsdatum = geburtsdatumDb)
//      }).toSeq
//      logger.info("Inserting " + users.mkString("\n"))
//      transaction {
//        tUser.insert(users)
//      }
//    }
//    def doPersonsXml(dir: File) {
//
//    }
//
//    logger.info("global")
//    delete(Abgabe, Code, Forum, Registrierung, Global)
//  }
//
//  def abgabe() {
//    val userIds = transaction { tUser.toArray.map(_.id).toSet }
//
//    // tasks.xml /tasks/task/subtask
//    def processSubtask(node: Node, task: AbgabeTask) {
//      val toInsert = AbgabeSubtask(subtask_id = node.attribute("id"), text = (node \ "text").text, task_id = task.id)
//      logger.info("Inserting " + toInsert)
//      val db = tAbgabeSubtask.insert(toInsert)
//    }
//
//    // tasks.xml /tasks
//    def processGroup(node: Node, abgabe: Abgabe) {
//      node \ "group" foreach { e =>
//        val toInsert = AbgabeGruppe(gruppe_id = e.attribute("id"), abgabe_id = abgabe.id)
//        logger.info("Inserting " + toInsert)
//        val abgabeGruppe = tAbgabeGruppe.insert(toInsert)
//        // Test
//        // <test id="1" desc="16.12.2008"/>
//        e \ "test" foreach { e =>
//          val toInsert = AbgabeTest(abgabe_gruppe_id = abgabeGruppe.id, test_id = e.attribute("id"), beschreibung = e.attribute("desc"))
//          logger.info("Inserting " + toInsert)
//          tAbgabeTest.insert(toInsert)
//        }
//      }
//    }
//
//    // tasks.xml /tasks
//    def processTasks(node: Node, abgabe: Abgabe) {
//      node \ "task" foreach { task =>
//        // Data of a single task
//        val id: Int = task.attribute("id")
//        val short: String = task.attribute("short")
//        val long: String = task.attribute("long")
//        // Aufgabenzuordung pro Gruppe
//        val groupTask = node \ "group" \ "task" find { e =>
//          val groupTaskId: Int = e.attribute("id")
//          groupTaskId == id
//        }
//        val (from, to, presence) = if (groupTask.isDefined) {
//          val f: Timestamp = groupTask.get.attribute("from")
//          val t: Timestamp = groupTask.get.attribute("to")
//          val p: LocalDate = groupTask.get.attribute("presence")
//          (Some(f), Some(t), Some(p))
//        } else (None, None, None)
//        val toInsert = AbgabeTask(task_id = id, kurzbezeichnung = short, beschreibung = long, abgabe_id = abgabe.id, beginn = from, ende = to, presence = presence.map(_.toDate()))
//        logger.info("Inserting " + toInsert)
//        val db = tAbgabeTask.insert(toInsert)
//        // Optional subtasks
//        task \ "subtask" foreach { e => processSubtask(e, db) }
//      }
//    }
//
//    def processAssessment(node: Node, abgabe: Abgabe) {
//      val userId: String = node.attribute("id")
//      if (!userIds.contains(userId)) {
//        logger.error(s"Unknown user id $userId in assessments.xml")
//      } else {
//        // plus and minus
//        val pluses: Seq[LocalDate] = node \ "plus" map { e =>
//          val date: LocalDate = e.attribute("date")
//          date
//        }
//        val minuses: Seq[LocalDate] = node \ "minus" map { e =>
//          val date: LocalDate = e.attribute("date")
//          date
//        }
//        val sum = pluses.size - minuses.size
//        val mitarbeit = AbgabeMitarbeit(plusminus = sum, abgabe_id = abgabe.id, user_id = userId)
//        logger.info("Inserting " + mitarbeit)
//        tAbgabeMitarbeit.insert(mitarbeit)
//        // results
//        val abgabeGruppen = tAbgabeGruppe.where(_.abgabe_id === abgabe.id).toArray
//        val abgabeTests = tAbgabeTest.where(_.abgabe_gruppe_id in abgabeGruppen.map(_.id).toSeq).toArray
//        node \ "result" foreach { result =>
//          val id: Int = result.attribute("id")
//          abgabeTests.filter(_.test_id == id) foreach { abgabeTest =>
//            val abgabeTestUser = AbgabeTestUser(abgabe_test_id = abgabeTest.id, user_id = userId, result = result.text.toFloat)
//            logger.info("Inserting " + abgabeTestUser)
//            tAbgabeTestUser.insert(abgabeTestUser)
//          }
//        }
//      }
//    }
//
//    def processFeedback(node: Node, abgabe: Abgabe) {
//      // fetch all tasks and subtask for the current abgabe
//      val tasksSubtasks = (from(tAbgabeTask, tAbgabeSubtask)((t, s) =>
//        where(t.abgabe_id === abgabe.id and t.id === s.task_id)
//          select (t, s))).toArray
//      logger.info("Tasks Subtasks " + tasksSubtasks.length)
//      node \ "person" foreach { person =>
//        val studentId = person.attribute("id")
//        person \ "comment" foreach { comment =>
//          val author: String = comment.attribute("author")
//          val subtask: Int = comment.attribute("subtask")
//          val task: Int = comment.attribute("task")
//          val subtaskId = tasksSubtasks.find(e => e._1.task_id == task && e._2.subtask_id == subtask).map(_._2.id).get
//          val abgabeFeedback = AbgabeFeedback(autor_id = author, student_id = studentId, subtask_id = subtaskId)
//          if (!userIds.contains(studentId)) {
//            logger.info("Unknown student id for abgabeFeedback " + studentId)
//          } else {
//            logger.info("Inserting " + abgabeFeedback)
//            tAbgabeFeedback.insert(abgabeFeedback)
//          }
//        }
//      }
//    }
//
//    def processUploads(abgabe: Abgabe) {
//      // fetch all tasks and subtask for the current abgabe
//      val tasksSubtasks = (from(tAbgabeTask, tAbgabeSubtask)((t, s) =>
//        where(t.abgabe_id === abgabe.id and t.id === s.task_id)
//          select (t, s))).toArray
//      logger.info("Tasks Subtasks " + tasksSubtasks.length)
//      val dataDir = new File(dirs(Abgabe), s"${abgabe.id}/Data")
//      if (!dataDir.isDirectory()) {
//        logger.error("No code uploads for abgabe " + abgabe + " as dir doesn't exist " + dataDir)
//      } else {
//        for {
//          userDir <- dataDir.listFiles(dirFilter)
//          userId = userDir.getName()
//          taskDir <- userDir.listFiles(dirFilter)
//          task = taskDir.getName().toInt
//          subtaskDir <- taskDir.listFiles(dirFilter)
//          subtask = subtaskDir.getName().toInt
//        } {
//          subtaskDir.listFiles(nonDirFilter) match {
//            case Array(file, _) =>
//              val zeitpunkt = new Timestamp(file.lastModified())
//              val subtaskId = tasksSubtasks.find(e => e._1.task_id == task && e._2.subtask_id == subtask).map(_._2.id).get
//              val abgabeUpload = AbgabeUpload(zeitpunkt = zeitpunkt, subtask_id = subtaskId, user_id = userId)
//              logger.info("Inserting " + abgabeUpload)
//              tAbgabeUpload.insert(abgabeUpload)
//            case _ =>
//          }
//        }
//      }
//    }
//
//    logger.info("abgabe")
//    delete(Abgabe)
//
//    transaction {
//      val instances = readInstances(new File(dirs(Abgabe), "descriptions.xml"))
//      val abgaben = instances.map { e =>
//        val toInsert = new Abgabe(id = e.id, name = e.name, lva_id = e.lva.id)
//        logger.info("Inserting " + toInsert)
//        tAbgabe.insert(toInsert)
//      }
//      abgaben.take(2) foreach { abgabe =>
//        def file(name: String) = new File(dirs(Code), s"${abgabe.id}/$name")
//        // tasks
//        checkAndReadFile(file("tasks.xml")) foreach { xml =>
//          processTasks(xml, abgabe)
//          processGroup(xml, abgabe)
//        }
//        // assessments
//        checkAndReadFile(file("assessments.xml")) foreach {
//          _ \ "person" foreach { e => processAssessment(e, abgabe) }
//        }
//        // feedback
//        checkAndReadFile(file("feedback.xml")) foreach { e =>
//          processFeedback(e, abgabe)
//        }
//        // uploads
//        processUploads(abgabe)
//      }
//
//    }
//  }
//
//  logger.info("start")
//  def forum() {
//    logger.info("forum")
//    delete(Forum)
//
//    val userIds = transaction { tUser.toArray.map(_.id).toSet }
//
//    def readAndInsertPostings(forumsId: Long, forumsbereichId: Long, forumsbereichDbid: Long) {
//      // recursive method
//      def process(node: Node, parent_id: Option[Long] = None) {
//        // Data of a single posting
//        val user: String = node.attribute("user")
//        val name: String = node.attribute("name")
//        val id: Int = node.attribute("id")
//        val date: Timestamp = node.attribute("date")
//        val betreff = (node \ "subject").text
//        val text = (node \ "text").text
//        if (userIds.contains(user)) {
//          val postingDb = {
//            val postingToInsert = Posting(postingid = id, datum = date, betreff_laenge = betreff.length(), text_laenge = text.length(), forumsbereich_id = forumsbereichDbid, parent_id = parent_id, user_id = user)
//            logger.info("Inserting " + postingToInsert)
//            tPosting.insert(postingToInsert)
//          }
//          // Optional answers
//          node \ "sub" \ "entry" foreach { e => process(e, Some(postingDb.id)) }
//        } else {
//          logger.error(s"Unknown user $user in forumsbereich $forumsbereichId in forum $forumsId")
//        }
//      }
//      val file = new File(dirs(Forum), s"$forumsId/Data/$forumsbereichId.xml")
//      if (file.exists()) {
//        val data = XML.loadFile(file)
//        data \ "entry" foreach { e => process(e) }
//      } else {
//        logger.error(s"No postings for forumsId $forumsId forumsbereichId $forumsbereichId")
//      }
//    }
//
//    def processReadProgress(forumsIds: Seq[Long]) {
//      val postingsForumsbereich = (from(tPosting, tForumsbereich)((p, f) =>
//        where(p.forumsbereich_id === f.id)
//          select (p, f))).toArray
//      forumsIds.map { forumsId =>
//        val dir = new File(dirs(Forum), s"$forumsId/User")
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
//          for {
//            readProgressFile <- personDir.listFiles(readProgressFilter)
//            bereichsId = readProgressFile.getName().toInt
//            userId = personDir.getName
//            postingId <- Source.fromFile(readProgressFile).getLines.map(_.toInt)
//          } {
//            postingsForumsbereich.find(e => e._2.forum_id == forumsId && e._2.bereichsid == bereichsId && e._1.postingid == postingId) match {
//              case Some((posting, _)) =>
//                val tToInsert = PostingRead(posting_id = posting.id, user_id = userId)
//                logger.info("Inserting " + tToInsert)
//                tPostingRead.insert(tToInsert)
//              case None =>
//                logger.error(s"No read progress for postingId $postingId forumsId $forumsId and userId $userId in file ${readProgressFile.getCanonicalPath()}")
//            }
//          }
//        }
//      }
//    }
//
//    def getForumsbereiche(forumsId: Long): Seq[Forumsbereich] =
//      XML.loadFile(new File(dirs(Forum), s"$forumsId/issues.xml")) \ "entry" map { e =>
//        val id: Int = e.attribute("id")
//        val name: String = e.attribute("what")
//        val beschreibung: String = (e \ "text").text
//        Forumsbereich(bereichsid = id, name = name, beschreibung = beschreibung, forum_id = forumsId)
//      }
//
//    val forums = transaction {
//      readInstances(new File(dirs(Forum), "descriptions.xml")).filter(_.id == 100).map { e =>
//        val toInsert = new Forum(id = e.id, langtext = e.name, lva_dbid = e.lva.id)
//        logger.info("Inserting " + toInsert)
//        tForum.insert(toInsert)
//      }
//    }
//    val forumsbereiche =
//      transaction {
//        val toInsert = forums.flatMap(e => getForumsbereiche(e.id))
//        logger.info("Inserting\n\t" + toInsert.mkString("-" * 30, "\n\t", "-" * 30))
//        tForumsbereich.insert(toInsert)
//        tForumsbereich.toArray
//      }
//    transaction {
//      forumsbereiche.foreach { e =>
//        readAndInsertPostings(forumsId = e.forum_id, forumsbereichId = e.bereichsid, forumsbereichDbid = e.id)
//      }
//      processReadProgress(forums.map(_.id))
//    }
//
//  }
//
//  def registrierung() {
//    logger.info("registrierung")
//    delete(Registrierung)
//
//    transaction {
//      val userIds = tUser.toArray.map(_.id).toSet
//      readInstances(new File(dirs(Registrierung), "descriptions.xml")) foreach { instance =>
//        val toInsert = new Registrierung(id = instance.id, name = instance.name, lva_dbid = instance.lva.id)
//        logger.info("Inserting " + toInsert)
//        val registrierung = tRegistrierung.insert(toInsert)
//
//        def file(name: String) = new File(registerDir, s"${registrierung.id}/$name")
//
//        // <group id="3" title="Anmeldung ...">
//        def processRegisterGruppe(node: Node) {
//        }
//
//        // register Gruppe
//        val registerGruppen = (checkAndReadFile(file("custom.xml")) map { xml =>
//          val (beginn, ende) = (xml \ "register" map { e =>
//            val beginn: Timestamp = e.attribute("begin-reg")
//            val ende: Timestamp = e.attribute("end-reg")
//            (beginn, ende)
//          }).head
//          val beginnEndeGruppen = (xml \ "register" \ "group" map { e =>
//            val id: Int = e.attribute("id")
//            id
//          }).toSet
//
//          xml \ "group" map { e =>
//            val id: Int = e.attribute("id")
//            val (beginnGruppe, endeGruppe) = if (beginnEndeGruppen.isEmpty || beginnEndeGruppen.contains(id)) {
//              (Some(beginn), Some(ende))
//            } else { (None, None) }
//            val toInsert = RegisterGruppe(gruppe_id = id, title = e.attribute("title"), registrierung_id = registrierung.id, beginn = beginnGruppe, ende = endeGruppe)
//            logger.info("Inserting " + toInsert)
//            tRegisterGruppe.insert(toInsert)
//          }
//        }).getOrElse(Seq())
//
//        // todo
//        checkAndReadFile(file("registrations.xml")) foreach { xml =>
//          xml \ "registration" foreach { e =>
//            val group: Int = e.attribute("group")
//            val userId: String = e.text
//            val slot: Int = e.attribute("slot")
//            // user_gruppe
//            (userIds.contains(userId), registerGruppen.find(_.gruppe_id == group)) match {
//              case (true, Some(registerGruppe)) =>
//                val userGruppe = UserGruppe(user_id = userId, register_gruppe_id = registerGruppe.id)
//                logger.info("Inserting " + userGruppe)
//                tUserGruppe.insert(userGruppe)
//              case (_, None) => logger.error(s"No register gruppe for group $group slot $slot userId $userId registrierung $registrierung")
//              case (false, _) => logger.error(s"Unknow user for group $group slot $slot userId $userId registrierung $registrierung")
//            }
//          }
//          // slots from registration
//          for {
//            node <- xml \ "registration"
//            group: Int = node.attribute("group")
//            slot: Int = node.attribute("slot")
//            unit: Int = node.attribute("unit")
//            userId: String = node.text
//            registerGruppe = registerGruppen.find(_.gruppe_id == group)
//            if (registerGruppe.isDefined)
//            if (userIds.contains(userId))
//          } {
//            // TODO Start End
//
//            val slotToInsert = Slot(slot_id = slot, unit = unit, start = None, ende = None, register_gruppe_id = registerGruppe.get.id)
//            logger.info("Inserting " + slotToInsert)
//            val slotDb = tSlot.insert(slotToInsert)
//            val userSlotToInsert = UserSlot(user_id = userId, slot_id = slotDb.id)
//            logger.info("Inserting " + userSlotToInsert)
//            tUserSlot.insert(userSlotToInsert)
//          }
//        }
//        // registrierung user
//        val data = (from(tRegistrierung, tRegisterGruppe, tUserGruppe)((r, rg, ug) =>
//          where(r.id === rg.registrierung_id and rg.id === ug.register_gruppe_id)
//            select (r, ug.user_id))).toArray
//        for ((registrierung, userId) <- data) {
//          val ru = RegistrierungUser(registrierung_id = registrierung.id, user_id = userId, name = registrierung.name, lva_id = registrierung.lva_dbid)
//          logger.info("Inserting " + ru)
//          tRegistrierungUser.insert(ru)
//        }
//
//      }
//
//    }
//
//  }
//  // Helper Methods
//  def checkAndReadFile(file: File): Option[Elem] = {
//    logger.info("checkAndReadFile " + file.getCanonicalPath())
//    if (file.exists())
//      Some(XML.loadFile(file))
//    else {
//      logger.error("Missing file " + file.getCanonicalPath())
//      None
//    }
//  }
//
//  def initDb {
//    Class.forName("com.mysql.jdbc.Driver");
//    SessionFactory.concreteFactory = Some(() =>
//      Session.create(
//        DriverManager.getConnection("jdbc:mysql://localhost/uni_business_intelligence?user=bi&password=bi"),
//        new MySQLAdapter))
//  }
//
//  case class Instance(id: Int, name: String, lva: Lva)
//
//  def readInstances(file: File): Seq[Instance] = {
//    val lvas = tLva.toArray
//    val pattern = """(?s)http://[^/]+/[^/]+/([^/]+)/.*(se\d\d)[^:]*:(.*)""".r
//    XML.loadFile(file) \ "instance" map { node =>
//      val id: Int = node.attribute("id")
//      val url: String = node.text
//      println("Checking " + url)
//      val pattern(lvaName, lvaSemester, beschreibung) = url
//      Instance(id = id, name = beschreibung, lva = lvas.find(e => e.name == lvaName && e.semester == lvaSemester).get)
//    }
//  }
//
//  // Program start
//  initDb
//  args match {
//    case Array("global") => global()
//    case Array("registrierung") => registrierung()
//    case Array("code") => code()
//    case Array("forum") => forum()
//    case Array("abgabe") => abgabe()
//    case Array("all") => {
//      global()
//      registrierung()
//      code()
//      forum()
//      abgabe()
//    }
//    case _ => println("Invalid parameters")
//  }
//}

