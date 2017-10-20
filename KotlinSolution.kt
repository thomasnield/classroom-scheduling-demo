import com.swa.np.common.util.asMinutes
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable
import java.time.LocalTime

val operatingDaysOfWeek = 1..6

val operatingTimes = listOf(
        LocalTime.of(8,0)..LocalTime.of(11,0),
        LocalTime.of(13,0)..LocalTime.of(17,0)
)

// break up week into discrete 15 minute increments
val incrementedTimeline = 0..(7 * 24 * 4)


// convert operating times to discrete 15 minute increments
val availableBlocks = operatingTimes.asSequence()
        .flatMap { timeRange ->

            val timeRangeConverted = timeRange.start.toDiscreteIntervals()..timeRange.endInclusive.toDiscreteIntervals()

            incrementedTimeline.asSequence()
                    .filter { (it / 96) in operatingDaysOfWeek }
                    .filter { (it % 96) in timeRangeConverted }
                    .groupBy { it / 96 }
                    .asSequence()
                    .map { AvailableBlock(it.key, it.value.min()!!..it.value.max()!!, timeRange) }
        }
        .sortedBy { it.discreteRange.start }
        .toList()

// declare model
val model = ExpressionsBasedModel()

fun main(args: Array<String>) {
    availableBlocks.forEach { println(it) }
}

fun LocalTime.toDiscreteIntervals() = asMinutes().toInt() / 15

data class AvailableBlock(val dayOfWeek: Int,
                          val discreteRange: IntRange,
                          val timeRange: ClosedRange<LocalTime> ) {

}

data class SchedClass(val id: Int,
                      val name: String,
                      val length: Int,
                      val repetitions: Int) {

    val classStart = Variable("$id-start")
    val classEnd = Variable("$id-end")

}
