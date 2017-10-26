import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger


// declare model
val model = ExpressionsBasedModel()

val funcId = AtomicInteger(0)
val variableId = AtomicInteger(0)
fun variable() = Variable(variableId.incrementAndGet().toString().let { "Variable$it" }).apply(model::addVariable)
fun ExpressionsBasedModel.addExpression() = funcId.incrementAndGet().let { "Func$it"}.let { model.addExpression(it) }


// Any Monday through Friday date range will work
val operatingDates = LocalDate.of(2017,10,16)..LocalDate.of(2017,10,20)

val operatingTimes = listOf(
        LocalTime.of(8,0)..LocalTime.of(11,30),
        LocalTime.of(13,0)..LocalTime.of(17,0)
)

// Operating DateTimes
val availableBlocks = generateSequence(operatingDates.start) {
    it.plusDays(1).takeIf { it <= operatingDates.endInclusive }
}.flatMap { dt ->
    operatingTimes.asSequence()
            .map { dt.atTime(it.start)..dt.atTime(it.endInclusive) }
}.map { AvailableRange(it) }
.toList()

val windowRange = availableBlocks.map { it.dateTimeRange.start }.min()!! .. availableBlocks.asSequence().map { it.dateTimeRange.endInclusive }.max()!!
val windowRangeDiscrete = windowRange.asDiscrete()
val windowLength = windowRangeDiscrete.asSequence().count()

val classesInput = listOf(
        RecurringClass(id=1, name="Psych 101", hoursLength=1.0, repetitions=2),
        RecurringClass(id=2, name="English 101", hoursLength=1.5, repetitions=2),
        RecurringClass(id=3, name="Math 300", hoursLength=1.5, repetitions=2),
        RecurringClass(id=4, name="Psych 300", hoursLength=3.0, repetitions=1),
        RecurringClass(id=5, name="Calculus I", hoursLength=2.0, repetitions=2),
        RecurringClass(id=6, name="Linear Algebra I", hoursLength=2.0, repetitions=3)
        //RecurringClass(id=7, name="Sociology 101", hoursLength=1.0, repetitions=2)
        //RecurringClass(id=8, name="Biology 101", hoursLength=1.0, repetitions=2)
)


data class AvailableRange(val dateTimeRange: ClosedRange<LocalDateTime>) {

    val discreteRange = dateTimeRange.asDiscrete()
}

data class RecurringClass(val id: Int,
                          val name: String,
                          val hoursLength: Double,
                          val repetitions: Int) {

    val scheduledSessions by lazy {
        (1..repetitions).asSequence()
            .map { ScheduledClass(id, name, hoursLength, it, this) }
            .toList()
    }
    fun addConstraints() = scheduledSessions.forEach { it.addConstraints() }
}

data class ScheduledClass(val id: Int,
                          val name: String,
                          val hoursLength: Double,
                          val repetitionIndex: Int,
                          val recurringClass: RecurringClass) {

    val discreteLength = (hoursLength * 4).toInt()

    val startDiscrete = variable().integer(true).lower(windowRangeDiscrete.start).upper(windowRangeDiscrete.endInclusive)
    val endDiscrete = variable().integer(true).lower(windowRangeDiscrete.start).upper(windowRangeDiscrete.endInclusive)

    val discreteRange get() = startDiscrete.value.toInt()..endDiscrete.value.toInt()
    val dateTimeRange get() = (startDiscrete.value.toInt()..endDiscrete.value.toInt()).asLocalDateTime()

    val date get() = dateTimeRange.first.toLocalDate()
    val timeRange get() = dateTimeRange.let { it.first.toLocalTime()..it.second.toLocalTime() }

    fun addConstraints() {

        //limit length of class
        model.addExpression()
                .level(discreteLength)
                .set(endDiscrete, 1)
                .set(startDiscrete, -1)

        // 2 <= E - S
        model.addExpression()
                .lower(2)
                .set(startDiscrete,-1)
                .set(endDiscrete, 1)

        // keep repetitions consecutive and 48 hours apart
        // Sj = Si + (48 * 4)
        recurringClass.scheduledSessions
                .asSequence()
                .filter { it.repetitionIndex == repetitionIndex - 1 }
                .forEach { otherSession ->

                    model.addExpression()
                            .level(48 * 4)
                            .set(startDiscrete, 1)
                            .set(otherSession.startDiscrete, -1)
                }

        // don't overlap with other classes
        classesInput.asSequence()
                .flatMap { it.scheduledSessions.asSequence() }
                .filter { it != this }
                .forEach { other ->
                    val overlapSwitch = variable().binary()

                    // Si >= Ej - 1000b
                    // 0 >= Ej - Si - 1000b
                    model.addExpression()
                            .upper(0)
                            .set(other.endDiscrete, 1)
                            .set(startDiscrete, -1)
                            .set(overlapSwitch,-windowLength)

                    // Sj >= Ei - 1000(1-b)
                    // 1000 >= Ei - Sj + 1000b
                    model.addExpression()
                            .upper(windowLength)
                            .set(endDiscrete, 1)
                            .set(other.startDiscrete, -1)
                            .set(overlapSwitch, windowLength)
                }



        /*
        // limit to allowable times
        availableBlocks.asSequence().flatMap { block ->
            availableBlocks.asSequence()
                    .filter { it != block }
                    .map { block to it }
        }.forEach { (block, otherblock) ->

            sequenceOf(startDiscrete, endDiscrete).forEach { classTime ->

                val binarySwitch =  variable().binary()

                // 1000b + Ei = 1000 + S
                model.addExpression()
                        .lower(block.discreteRange.start - windowLength)
                        .set(classTime, 1)
                        .set(binarySwitch, - windowLength)

                model.addExpression()
                        .upper(block.discreteRange.endInclusive - windowLength)
                        .set(classTime, 1)
                        .set(binarySwitch, - windowLength)

                model.addExpression()
                        .lower(otherblock.discreteRange.start)
                        .set(classTime, 1)
                        .set(binarySwitch, windowLength)

                model.addExpression()
                        .upper(otherblock.discreteRange.endInclusive)
                        .set(classTime, 1)
                        .set(binarySwitch, -windowLength)
            }
        }
*/
    }
}


fun ClosedRange<LocalDateTime>.asDiscrete() = generateSequence(this.start) {
    it.plusMinutes(15).takeIf { it <= this.endInclusive }
}.map { ChronoUnit.MINUTES.between(operatingDates.start.atTime(operatingTimes.first().start), it) }
        .map { it / 15 }
        .toList()
        .let { it.min()!!.toInt()..it.max()!!.toInt() }


fun IntRange.asLocalDateTime() = asSequence()
        .map { it * 15 }
        .map { windowRange.start.plusMinutes(it.toLong()) }
        .let { it.min()!! to it.max()!! }


fun main(args: Array<String>) {

    classesInput.forEach { it.addConstraints() }

    classesInput.asSequence()
            .filter { it.repetitions == 3 }
            .flatMap { it.scheduledSessions.asSequence() }
            .filter { it.repetitionIndex == 1 }
            .forEach {
                model.addExpression()
                        .lower(availableBlocks[0].discreteRange.start)
                        .upper(availableBlocks[1].discreteRange.endInclusive)
                        .set(it.startDiscrete, 1)

                model.addExpression()
                        .lower(availableBlocks[0].discreteRange.start)
                        .upper(availableBlocks[1].discreteRange.endInclusive)
                        .set(it.endDiscrete, 1)
            }


    classesInput.asSequence()
            .filter { it.repetitions == 2 }
            .take(3)
            .flatMap { it.scheduledSessions.asSequence() }
            .filter { it.repetitionIndex == 1 }
            .forEach {
                model.addExpression()
                        .lower(availableBlocks[2].discreteRange.start)
                        .upper(availableBlocks[3].discreteRange.endInclusive)
                        .set(it.startDiscrete, 1)

                model.addExpression()
                        .lower(availableBlocks[2].discreteRange.start)
                        .upper(availableBlocks[3].discreteRange.endInclusive)
                        .set(it.endDiscrete, 1)
            }

    println(model.minimise())

    classesInput.asSequence()
            .flatMap { it.scheduledSessions.asSequence() }
            .sortedBy { it.dateTimeRange.first }.forEach {
                println("${it.name}-${it.repetitionIndex} ${it.date.dayOfWeek} ${it.timeRange}")
            }
}