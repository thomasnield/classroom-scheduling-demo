import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Any Monday through Friday date range will work
val operatingDates = LocalDate.of(2017,10,16)..LocalDate.of(2017,10,20)

val operatingTimes = listOf(
        LocalTime.of(8,0)..LocalTime.of(11,0),
        LocalTime.of(13,0)..LocalTime.of(17,0)
)


val calendarStart = operatingDates.start.with(DayOfWeek.MONDAY)

// Operating DateTimes
val availableBlocks = generateSequence(operatingDates.start) {
    it.plusDays(1).takeIf { it <= operatingDates.endInclusive }
}.flatMap { dt ->
    operatingTimes.asSequence()
            .map { dt.atTime(it.start)..dt.atTime(it.endInclusive) }
}.map { AvailableBlock(it) }
 .toList()


// declare model
val model = ExpressionsBasedModel()

fun main(args: Array<String>) {

}

data class AvailableBlock(val dateTimeRange: ClosedRange<LocalDateTime>) {

}

data class SchedClass(val id: Int,
                      val name: String,
                      val length: Int,
                      val repetitions: Int) {

    val classStart = Variable("$id-start")
    val classEnd = Variable("$id-end")

}
