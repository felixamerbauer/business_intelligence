package bi

import java.io.File
import java.io.FileFilter
import java.util.Date
import org.joda.time.LocalDateTime
import org.squeryl.PrimitiveTypeMode.transaction
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
import bi.Implicits.localDateTime2Timestamp
import scala.io.Source

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
      transaction {
        tUser.insert(users)
      }
    }
    def doPersonsXml(dir: File) {

    }

    logger.info("global")
    delete(AAbgabe, ACode, AForum, ARegistrierung, AGlobal)
    doMetaCsv
  }
}