import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun getDateTime(date: Long): Pair<String, String> {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    val instant = Instant.ofEpochSecond(date)
    val date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return Pair(dateFormatter.format(date), timeFormatter.format(date))
}