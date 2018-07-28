import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

// declare model
val model = ExpressionsBasedModel()


// improvised DSL
val funcId = AtomicInteger(0)
val variableId = AtomicInteger(0)
fun variable() = Variable(variableId.incrementAndGet().toString().let { "Variable$it" }).apply(model::addVariable)
fun addExpression() = funcId.incrementAndGet().let { "Func$it"}.let { model.addExpression(it) }


/** A discrete, 15-minute chunk of time a class can be scheduled on */
data class Block(val dateTimeRange: ClosedRange<LocalDateTime>) {

    val timeRange = dateTimeRange.let { it.start.toLocalTime()..it.endInclusive.toLocalTime() }

    /** indicates if this block is zeroed due to operating day/break constraints */
    val withinOperatingDay get() =  breaks.all { timeRange.start !in it } &&
            timeRange.start in operatingDay &&
            timeRange.endInclusive in operatingDay

    fun addConstraints() {
        if (withinOperatingDay) {
            addExpression().lower(0).upper(1).apply {
                ScheduledClass.all.asSequence().flatMap { it.affectingSlotsFor(this@Block) }
                        .filter { it.block.withinOperatingDay }
                        .forEach {
                            set(it.occupied, 1)
                        }
            }
        } else {
            ScheduledClass.all.asSequence().flatMap { it.affectingSlotsFor(this@Block) }
                    .forEach {
                        it.occupied.level(0)
                    }
        }
    }

    companion object {

        /* All operating blocks for the entire week, broken up in 15 minute increments */
        val all by lazy {
            generateSequence(operatingDates.start.atStartOfDay()) {
                it.plusMinutes(15).takeIf { it.plusMinutes(15) <= operatingDates.endInclusive.atTime(23,59) }
            }.map { Block(it..it.plusMinutes(15)) }
             .toList()
        }

        fun applyConstraints() {
            all.forEach { it.addConstraints() }
        }
    }
}


data class ScheduledClass(val id: Int,
                          val name: String,
                          val hoursLength: Double,
                          val repetitions: Int,
                          val repetitionGapDays: Int = 2) {

    /** the # of slots between each recurrence */
    val gapLengthInSlots = repetitionGapDays * 24 * 4

    /** the # of slots needed for a given occurrence */
    val slotsNeeded = (hoursLength * 4).toInt()

    /** yields slots for this given scheduled class */
    val slots by lazy {
        Slot.all.asSequence().filter { it.scheduledClass == this }.toList()
    }

    /** yields slot groups for this scheduled class */
    val slotGroups by lazy {
        slots.rollingRecurrences(slotsNeeded = slotsNeeded, gap = gapLengthInSlots, recurrences = repetitions)
    }

    /** yields slots that affect the given block for this scheduled class */
    fun affectingSlotsFor(block: Block) = slotGroups.asSequence()
            .filter { it.flatMap { it }.any { it.block == block } }
            .map { it.first().first() }

    /** translates and returns the optimized start time of the class */
    val start get() = slots.asSequence().filter { it.occupied.value.toInt() == 1 }.map { it.block.dateTimeRange.start }.min()!!

    /** translates and returns the optimized end time of the class */
    val end get() = start.plusMinutes((hoursLength * 60.0).toLong())

    /** returns the DayOfWeeks where recurrences take place */
    val daysOfWeek get() = (0..(repetitions-1)).asSequence().map { start.dayOfWeek.plus(it.toLong() * repetitionGapDays) }.sorted()

    fun addConstraints() {

        //sum of all slots for this scheduledClass must be 1
        // s1 + s2 + s3 .. + sn = 1
        addExpression().level(1).apply {
            slots.forEach {
                set(it.occupied, 1)
            }
        }

        //guide Mon/Wed/Fri for three repetitions
        if (repetitions == 3) {
            addExpression().level(1).apply {
                slots.filter { it.block.dateTimeRange.start.dayOfWeek == DayOfWeek.MONDAY }
                        .forEach {
                            set(it.occupied, 1)
                        }
            }
        }

        //guide two repetitions to start on Mon, Tues, or Wed
        if (repetitions == 2) {
            addExpression().level(1).apply {
                slots.filter { it.block.dateTimeRange.start.dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.WEDNESDAY }.forEach {
                    set(it.occupied, 1)
                }
            }
        }
    }

    companion object {
        val all by lazy { scheduledClasses }
    }
}



data class Slot(val block: Block, val scheduledClass: ScheduledClass) {
    val occupied = variable().apply { if (block.withinOperatingDay) binary() else level(0) }

    companion object {

        val all by lazy {
            Block.all.asSequence().flatMap { b ->
                ScheduledClass.all.asSequence().map { Slot(b,it) }
            }.toList()
        }
    }
}


fun applyConstraints() {
    Block.applyConstraints()
    ScheduledClass.all.forEach { it.addConstraints() }
}

fun <T> List<T>.rollingBatches(batchSize: Int) = (0..size).asSequence().map { i ->
    subList(i, (i + batchSize).let { if (it > size) size else it })
}.filter { it.size == batchSize }

fun <T> List<T>.rollingRecurrences(slotsNeeded: Int, gap: Int, recurrences: Int) =
        (0..size).asSequence().map { i ->
            (1..recurrences).asSequence().map { (it - 1) * gap }
                    .filter { it + i < size}
                    .map { r ->
                        subList(i + r, (i + r + slotsNeeded).let { if (it > size) size else it })
                    }.filter { it.size == slotsNeeded }
                    .toList()
        }.filter { it.size == recurrences }
