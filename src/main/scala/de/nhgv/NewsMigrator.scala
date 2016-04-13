package de.nhgv

import java.io.{BufferedWriter, File, FileWriter}
import java.sql.Timestamp
import java.time.LocalDateTime

import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._

/**
  * Created by timolitzius on 04.04.16.
  */
object NewsMigrator extends App {
  val sourceFilePath = "news.json"
  val targetPath = "/Users/timolitzius/Development/Projects/heimatverein-niederjosbach/content/aktuelles/"
  implicit val formats = DefaultFormats

  case class Row(title: String, content: String, date: String)

  case class Container(rows: Seq[Row])

  case class NewsItem(title: String, content: String, date: LocalDateTime, hasGallery: Boolean, fileName: String)

  def readJson: Container = {
    val fileContent: String = scala.io.Source.fromInputStream(getClass().getClassLoader().getResourceAsStream(sourceFilePath)).mkString
    val json = parse(fileContent)
    json.extract[Container]
  }

  def createNewsItems: Seq[NewsItem] = {
    readJson.rows.map { row =>
      // hackathy hack: add 2 hours...
      val combinedDate = row.date.toLong * 1000 + 7200000
      val offsettedDate = if (combinedDate.toString.endsWith("0")) combinedDate + 1 else combinedDate

      val date = new Timestamp(offsettedDate).toLocalDateTime
      def convertNameToFilename(s: String) = {
        s.toLowerCase
          .replaceAll("-", "") // replace minus in "dickworzschnitzen - der bericht"
          .replaceAll("\\*", "")
          .replaceAll(":", "")
          .replaceAll("!", "")
          .replaceAll(" ", "-")
          .replaceAll("\\(", "-")
          .replaceAll("\\)", "-")
          .replaceAll("ü", "ue")
          .replaceAll("ö", "oe")
          .replaceAll("ä", "ae")
          .replaceAll("ß", "ss")
          .replaceAll("\\.", "-")
          .replaceAll("\"", "")
          .replaceAll("/", "")
          .replaceAll("--", "-")
          .replaceAll("---", "-")
      }

      val month = if (date.getMonthValue < 10 ) s"0${date.getMonthValue}" else date.getMonthValue.toString
      val day = if (date.getDayOfMonth < 10 ) s"0${date.getDayOfMonth}" else date.getDayOfMonth.toString

      val content = row.content.replaceAll("""<(?!\/?a(?=>|\s.*>))\/?.*?>""", "")
        .replaceAll("&quot;", "\"")
        .replaceAll("&nbsp;", " ")

      NewsItem(title = row.title,
        content = content,
        date = date,
        hasGallery = row.content.contains("yag-gallery"),
        fileName = s"${date.getYear}/${convertNameToFilename(row.title)}")
    }
  }

  val newsItems = createNewsItems
  newsItems.foreach(println)

  val years = newsItems.groupBy(item => item.date.getYear.toString)
  years.map { year =>
    val currentYear = year._1
    val file = new File(s"${targetPath}${currentYear}")
    if (!file.exists) {
      file.mkdir()
    }
  }


  newsItems.foreach { item =>
    val file = new File(s"${targetPath}${item.fileName}.md")
    val bw = new BufferedWriter(new FileWriter(file))
    val galerieLine = if (item.hasGallery) s"galerie = '${item.hasGallery}'" else ""

    val fileContent =
      s"""+++
          |date = "${item.date}+02:00"
          |title = '${item.title}'
          |${galerieLine}
          |
        |+++
          |
          |${item.content}
          |
      """.stripMargin

    bw.write(fileContent)
    bw.close()
  }

}
