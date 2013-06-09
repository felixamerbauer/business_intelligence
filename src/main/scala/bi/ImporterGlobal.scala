package bi

import java.io.File
import java.io.FileFilter
import java.util.Date
import org.joda.time.LocalDateTime
import org.squeryl.PrimitiveTypeMode._
import com.typesafe.scalalogging.slf4j.Logging
import bi.Aspects._
import bi.DBSchema.tCodeUpload
import bi.DBSchema.tUser
import bi.Util.delete
import bi.Util.dirFilter
import bi.Util.dirs
import bi.Util.matrnrRegex
import bi.Util.nonDirFilter
import bi.Util.dataDir
import bi.Util.sdf
import bi.Implicits._
import scala.io.Source
import scala.xml.XML

object ImporterGlobal extends Logging {
  def run() {
    def doMetaCsv {
      val metaCsv = Source.fromFile(new File(dataDir, "meta.csv"))("Cp1252").mkString
      val metaCsvRegex = """(?m)^([^;]+);([^;]+);([^;]+);"([^;]+)"$""".r
      val geburtdatumRegx = """\d{6}""".r
      val users = (for (metaCsvRegex(matrNr, geschlecht, geburtsdatum, name) <- metaCsvRegex.findAllIn(metaCsv)) yield {
        val geburtsdatumDb = geburtdatumRegx.findFirstMatchIn(geburtsdatum).map { e =>
          sdf.parseLocalDate(geburtsdatum).toDate()
        }
        User(id = matrNr, email = None, name = Some(name), admin = None, geschlecht = Some(geschlecht), geburtsdatum = geburtsdatumDb)
      }).toSeq
      logger.info("Inserting " + users.mkString("\n"))
      tUser.insert(users)
    }
    def doPersonsXml(existingUsers: Seq[User], file: File) {
      XML.loadFile(file) \ "person" foreach { e =>
        val id: String = e.attribute("id")
        val name: String = e.attribute("name")
        val email: String = e.attribute("email")
        val admin: Boolean = e.attribute("admin")
        existingUsers.find(_.id == id) match {
          case Some(existing) => tUser.update(existing.copy(admin = Some(admin)))
          case _ =>
            val toInsert = User(id = id, email = Some(email), name = Some(name), admin = Some(admin), geschlecht = None, geburtsdatum = None)
            logger.info("inserting " + toInsert)
            tUser.insert(toInsert)
        }

      }
    }

    logger.info("global")
    delete(AAbgabe, ACode, AForum, ARegistrierung, AGlobal)
    transaction {
      doMetaCsv
      val users = tUser.toArray
      for {
        serviceDir <- dirs.values
        instanceDir <- serviceDir.listFiles(dirFilter)
        file <- instanceDir.listFiles()
        if (file.getName() == "persons.xml")
      } {
        doPersonsXml(users, file)
      }
    }
  }
}