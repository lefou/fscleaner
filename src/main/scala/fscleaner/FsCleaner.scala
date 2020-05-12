package fscleaner

import java.io.File

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.immutable.List
import scala.collection.immutable.Nil
import scala.collection.immutable.Seq

import com.typesafe.config.ConfigFactory

import de.tototec.cmdoption.CmdOption
import de.tototec.cmdoption.CmdlineParser

case class CleanConfig(
  deletes: Seq[String] = Seq(),
  deletePatterns: Seq[String] = Seq())

object FsCleaner {

  class Cmdline {
    @CmdOption(names = Array("--delete", "-d"), description = "Actually delete")
    var delete: Boolean = false
    @CmdOption(names = Array("--help", "-h"), description = "Show this help")
    var help: Boolean = false
    @CmdOption(names = Array("--recursive", "-r"), description = "Work recursively for subdirectories")
    var recursive: Boolean = false

    @CmdOption(args = Array("dir"), maxCount = -1)
    def dirs(dir: String): Unit = dirs +:= dir
    var dirs: Seq[String] = Seq()
  }

  def main(args: Array[String]): Unit = {

    val cmdline = new Cmdline()
    val cp = new CmdlineParser(cmdline)
    cp.setProgramName("fscleaner")
    cp.parse(args: _*)

    if (cmdline.help) {
      cp.usage()
      sys.exit(0)
    }

    val dirs = cmdline.dirs match {
      case Seq() => Seq(".")
      case d => d
    }

    dirs.foreach { dir =>
      val cwd = new File(new File(dir).getAbsoluteFile().toURI().normalize())
      if (!cwd.exists()) {
        sys.error(s"${cwd} does not exists")
      }
      val fsCleaner = new FsCleaner(cwd, cmdline.delete, cmdline.recursive)
      fsCleaner.clean()
    }

  }

}

class FsCleaner(baseDir: File, delete: Boolean, recursive: Boolean) {

  def relativePath(file: File): String = baseDir.toURI().relativize(file.toURI()).toString()

  def clean(): Unit = {
    // TODO: also read configs from above
    cleanDir(baseDir, Seq())
  }

  case class DeleteCount(dirs: Long, files: Long) {
    def +(dc: DeleteCount) = DeleteCount(dirs + dc.dirs, files + dc.files)
    override def toString(): String = s"(dirs: ${dirs}, files: ${files})"
  }

  def fileParts(file: File): List[String] = file match {
    case null => Nil
    case file => file.getName() :: fileParts(file.getParentFile())
  }

  //  def topMostParent(relFile: File): Option[String] = relFile match {
  //    case null => None
  //    case parent => topMostParent(parent).orElse(Some(parent.getName()))
  //  }

  def cleanDir(cwd: File, deletePatterns: Seq[String] = Seq()): Unit = if (cwd.exists() && cwd.isDirectory()) {

    val config = detectConfig(cwd)

    val newDeletePattern = deletePatterns ++ config.deletePatterns

    val deletes = config.deletes ++ newDeletePattern

    // TODO: do not delete resources also in ignore

    deletes.map { path =>
      val file = new File(cwd, path)
      if (file.exists()) {
        if (delete)
          print(s"Deleting ${relativePath(file)} ")
        else
          print(s"I would delete ${relativePath(file)} ")
        val dc = deleteRecursive(file)
        println(dc)
      }
    }

    if (recursive) {
      Option(cwd.listFiles()).getOrElse(Array()).foreach { file =>
        if (file.isDirectory()) {
          cleanDir(cwd = file, deletePatterns = newDeletePattern)
        }
      }
    }

  }

  def deleteRecursive(files: File*): DeleteCount = files.foldLeft(DeleteCount(0, 0)) { (dc, file) =>
    val addDc = if (file.isDirectory()) {
      DeleteCount(1, 0) + deleteRecursive(Option(file.listFiles()).getOrElse(Array()): _*)
    } else DeleteCount(0, 1)
    if (delete) file.delete()
    dc + addDc
  }

  def detectConfig(dir: File): CleanConfig = {
    val cf = new File(dir, ".fscleanerrc")
    if (cf.exists()) {
      val cfg = ConfigFactory.parseFile(cf).resolve()

      def stringList(key: String): Seq[String] =
        if (cfg.hasPath(key)) cfg.getStringList(key).asScala.toList
        else Seq()

      CleanConfig(
        deletes = stringList("delete"),
        deletePatterns = stringList("pattern.delete")
      )
    } else CleanConfig()
  }

}