package de.nhgv

import java.io.{FileWriter, BufferedWriter, File}
import java.sql.Timestamp
import java.time.LocalDateTime

import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._

/**
  * Created by timolitzius on 04.04.16.
  */
object EventMigrator extends App {
  val sourceFilePath = "events.json"
  val targetPath = "/Users/timolitzius/Development/Projects/heimatverein-niederjosbach/content/termine/"
  implicit val formats = DefaultFormats

  case class Row(title: String, startDate: String, startTime: String, allday: String, categoryTitle: String, locationName: String)

  case class Container(rows: Seq[Row])

  case class EventItem(title: String, date: LocalDateTime, ganztaegig: Boolean, verein: String, ort: String, fileName: String)

  def readJson: Container = {
    val fileContent: String = scala.io.Source.fromInputStream(getClass().getClassLoader().getResourceAsStream(sourceFilePath)).mkString
    val json = parse(fileContent)
    json.extract[Container]
  }

  def createEventItems: Seq[EventItem] = {
    readJson.rows.map { row =>
      val combinedDate = (row.startDate.toLong + row.startTime.toLong) * 1000
      val offsettedDate = if (combinedDate.toString.endsWith("0")) combinedDate + 1 else combinedDate

      val date = new Timestamp(offsettedDate).toLocalDateTime
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
          .replaceAll("/", "")
      }

      val month = if (date.getMonthValue < 10 ) s"0${date.getMonthValue}" else date.getMonthValue.toString
      val day = if (date.getDayOfMonth < 10 ) s"0${date.getDayOfMonth}" else date.getDayOfMonth.toString

      EventItem(title = row.title,
        date = date,
        ganztaegig = if (row.allday == "1") true else false,
        verein = row.categoryTitle,
        ort = row.locationName,
        fileName = s"${date.getYear}/${month}_${day}_${convertNameToFilename(row.title)}")
    }
  }

  val eventItems = createEventItems
  eventItems.foreach(println)

  val years = eventItems.groupBy(item => item.date.getYear.toString)
  years.map { year =>
    val currentYear = year._1
    val file = new File(s"${targetPath}${currentYear}")
    if (!file.exists) {
      file.mkdir()
    }
  }


  eventItems.foreach { item =>
    val file = new File(s"${targetPath}${item.fileName}.md")
    val bw = new BufferedWriter(new FileWriter(file))


    val fileContent =
      s"""+++
          |date = "${item.date}+02:00"
          |title = '${item.title}'
          |ganztaegig = '${item.ganztaegig}'
          |verein = '${item.verein}'
          |ort = '${item.ort}'
          |
        |+++
          |
      """.stripMargin

    bw.write(fileContent)
    bw.close()
  }

}
