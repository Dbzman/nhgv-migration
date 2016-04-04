package de.nhgv

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Paths}
import java.sql.Timestamp
import java.time.LocalDateTime

import org.json4s._
import org.json4s.native.JsonMethods._

/**
  * Created by timolitzius on 02.04.16.
  */
object GalleryMigrator extends App {
  val sourceFilePath = "gallery.json"
  val targetPath = "/Users/timolitzius/Development/Projects/heimatverein-niederjosbach/content/galerie/"
  implicit val formats = DefaultFormats

  case class Row(name: String, date: String, galleryImage: String)
  case class Container(rows: Seq[Row])
  case class GalleryItem(name: String, date: LocalDateTime, galleryPath: String, galleryImage: String)

  def readJson: Container = {
    val fileContent: String = scala.io.Source.fromInputStream(getClass().getClassLoader().getResourceAsStream(sourceFilePath)).mkString
    val json = parse(fileContent)
    json.extract[Container]
  }

  def createGalleryItems: Seq[GalleryItem] = {
    readJson.rows.map { row =>
      val rawDate = if (row.date.endsWith("0")) row.date.toLong + 1 else row.date.toLong
      val date = new Timestamp(rawDate * 1000).toLocalDateTime
      def convertNameToFilename(s: String) = {
        s.toLowerCase
          .replaceAll(" ", "_")
          .replaceAll("ü", "ue")
          .replaceAll("ö", "oe")
          .replaceAll("ä", "ae")
          .replaceAll("ß", "ss")
          .replaceAll("\\.", "")
          .replaceAll("\"", "")
          .replaceAll("-", "_")
      }

      GalleryItem(row.name, date, s"${date.getYear}/${convertNameToFilename(row.name)}", row.galleryImage)
    }
  }

  val galleryItems = createGalleryItems
  galleryItems.foreach(println)

  val years = galleryItems.groupBy(item => item.date.getYear.toString)
  years.map{ year =>
    val currentYear = year._1
    val file = new File(s"${targetPath}${currentYear}")
    if (!file.exists) {
      file.mkdir()
    }
  }


  galleryItems.foreach { item =>
    val file = new File(s"${targetPath}${item.galleryPath}.md")
    val bw = new BufferedWriter(new FileWriter(file))


    val fileContent =
      s"""+++
        |date = "${item.date}+02:00"
        |title = '${item.name}'
        |galerie_pfad = '${item.galleryPath}'
        |galerie_bild = '${item.galleryImage}'
        |
        |+++
        |
      """.stripMargin

    bw.write(fileContent)
    bw.close()
  }



}
