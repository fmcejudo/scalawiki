package org.scalawiki.bots

import java.nio.file.{Files, Paths}

import better.files.{File => SFile}
import org.scalawiki.AlphaNumOrdering

import scala.io.{Codec, Source}

/**
  * Save and load lines to/from file
  */
object FileUtils {

  val nl = System.lineSeparator

  /**
    * Read lines from file
    *
    * @param filename file to read
    * @param codec    character encoding/decoding preferences, default is [[scala.io.Codec.defaultCharsetCodec()]]
    * @return lines from the file
    */
  def read(filename: String)(implicit codec: Codec): Seq[String] =
    Source.fromFile(filename).getLines.toBuffer

  /**
    * Save lines to file, overwriting it
    *
    * @param filename file to write to
    * @param lines    lines to save
    * @param codec    character encoding/decoding preferences, default is [[scala.io.Codec.defaultCharsetCodec()]]
    * @return
    */
  def write(filename: String, lines: Seq[String])(implicit codec: Codec) =
    Files.write(Paths.get(filename), lines.mkString(nl).getBytes(codec.charSet))

  /**
    * @param dir directory
    * @return subdirectories, sorted by name
    */
  def subDirs(dir: SFile): Seq[SFile] =
    list(dir, _.isDirectory)

  /**
    * @param dir directory
    * @return regular files, sorted by name
    */
  def getFiles(dir: SFile): Seq[SFile] =
    list(dir, _.isRegularFile)

  /**
    * @param dir directory
    * @param predicate
    * @return directory members filtered by predicate
    */
  def list(dir: SFile, predicate: SFile => Boolean): Seq[SFile] =
    dir.list.filter(predicate).toSeq.sortBy(_.name)(AlphaNumOrdering)

  def isImage(f: SFile): Boolean = hasExt(f, Set(".jpg", ".tif"))

  def isDoc(f: SFile): Boolean = hasExt(f, Set(".doc", ".docx"))

  def isHtml(f: SFile): Boolean = hasExt(f, Set(".htm", ".html"))

  def hasExt(file: SFile, extensions: Set[String]): Boolean =
    extensions.contains(file.extension.getOrElse("."))

}
