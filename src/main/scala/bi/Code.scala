package bi

import java.io.File
import java.io.FileFilter
import java.util.Date

import scala.Array.canBuildFrom
import scala.Array.fallbackCanBuildFrom

import org.joda.time.LocalDateTime
import org.squeryl.PrimitiveTypeMode.transaction

import com.typesafe.scalalogging.slf4j.Logging

import bi.Aspect.Code
import bi.MySchema.tCodeUpload
import bi.Util.delete
import bi.Util.dirFilter
import bi.Util.dirs
import bi.Util.localDateTime2Timestamp
import bi.Util.matrnrRegex
import bi.Util.nonDirFilter

object ImporterCode extends Logging {
  def run() {
    logger.info("code")
    val matrnrDirFilter = new FileFilter {
      override def accept(file: File) = file.isDirectory() && matrnrRegex.findFirstIn(file.getName()).isDefined
    }
    def getInstances: Seq[Int] = dirs(Code).listFiles(dirFilter).map(_.getName().toInt)

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
        matrnrDir <- new File(dirs(Code), s"$instance/Data/1").listFiles(matrnrDirFilter)
        currentFile = getLatestCodeArtefact(matrnrDir)
        if (currentFile.isDefined)
      } yield {
        logger.debug("Checking " + matrnrDir.getCanonicalPath())

        (matrnrDir.getName(), LocalDateTime.fromDateFields(new Date(currentFile.get.lastModified())))
      }
    }
    delete(Code)
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
  }

}