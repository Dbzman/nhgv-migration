package de.nhgv

import java.io.{BufferedWriter, File, FileWriter}
import java.sql.Timestamp
import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}

import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._

/**
  * Created by timolitzius on 04.04.16.
  */
object EventImporter extends App {
  val sourceFilePath = "events_2018.json"
  val targetPath = "/Users/timolitzius/Development/Projects/heimatverein-niederjosbach/content/termine/"
  implicit val formats = DefaultFormats

  case class Row(Datum: String, Uhrzeit: String, Stadtteil: String, Ort: String, Name: String, Verein: String)

  case class Container(rows: Seq[Row])

  case class EventItem(title: String, date: LocalDateTime, verein: String, ort: String, stadtteil: String, fileName: String)

  def readJson: Container = {
    val fileContent: String = scala.io.Source.fromInputStream(getClass().getClassLoader().getResourceAsStream(sourceFilePath)).mkString
    val json = parse(fileContent)
    json.extract[Container]
  }

  def createEventItems: Seq[EventItem] = {
    readJson.rows.map { row =>
      // hackathy hack: add 2 hours...
//      val combinedDate = (row.startDate.toLong + row.startTime.toLong) * 1000 + 7200000
//      val offsettedDate = if (combinedDate.toString.endsWith("0")) combinedDate + 1 else combinedDate

      val dateParts = row.Datum.split("\\.").map(_.toInt)
      val timeParts = if(row.Uhrzeit.nonEmpty) row.Uhrzeit.split("\\:").map(_.toInt) else Array(0, 0)

      val date = LocalDateTime.of(dateParts(2), dateParts(1), dateParts(0), timeParts(0), timeParts(1))
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

      EventItem(title = row.Name,
        date = date,
        verein = row.Verein,
        ort = row.Ort,
        stadtteil = row.Stadtteil,
        fileName = s"${date.getYear}/${month}_${day}_${convertNameToFilename(row.Name)}")
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

    val location = if(item.ort.nonEmpty) s"${item.ort}, ${item.stadtteil}" else ""

    val fileContent =
      s"""+++
          |date = "${item.date}:00.000+02:00"
          |title = '${item.title}'
          |verein = '${item.verein}'
          |ort = '${location}'
          |
        |+++
          |
      """.stripMargin

    bw.write(fileContent)
    bw.close()
  }

}
