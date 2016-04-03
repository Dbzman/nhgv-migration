import java.sql.Timestamp
import java.time.format.DateTimeFormatter
val date = "1224952200"
if (date.endsWith("00")) date.toLong + 1 else date.toLong

val rawTimestamp = 1408648213L * 1000

new Timestamp(rawTimestamp).toLocalDateTime