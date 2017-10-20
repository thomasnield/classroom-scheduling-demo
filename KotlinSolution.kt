// Still a work-in-progress

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
val operatingTimesConverted = operatingTimes.asSequence()
        .flatMap { timeRange ->

            val timeRangeConverted = timeRange.start.toDiscreteIntervals()..timeRange.endInclusive.toDiscreteIntervals()

            incrementedTimeline.asSequence()
                    .filter { (it % 96) in timeRangeConverted }
                    .groupBy { it / 96 }
                    .values.asSequence()
        }.map { it.min()!!..it.max()!! }

// declare model
val model = ExpressionsBasedModel()

fun main(args: Array<String>) {
    operatingTimesConverted.forEach { println(it) }
}

fun LocalTime.toDiscreteIntervals() = asMinutes().toInt() / 15

data class SchedClass(val id: Int, val name: String, val length: Int) {

    val classStart = Variable("$id-start")
    val classEnd = Variable("$id-end")

}
