import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

// declare model
val model = ExpressionsBasedModel()

val funcId = AtomicInteger(0)
val variableId = AtomicInteger(0)
fun variable() = Variable(variableId.incrementAndGet().toString().let { "Variable$it" }).apply(model::addVariable)
fun addExpression() = funcId.incrementAndGet().let { "Func$it"}.let { model.addExpression(it) }



data class Block(val dateTimeRange: ClosedRange<LocalDateTime>) {

    val timeRange = dateTimeRange.let { it.start.toLocalTime()..it.endInclusive.toLocalTime() }

    val available get() =  (breaks.all { timeRange.start !in it } && timeRange.start in operatingDay)

    //val cumulativeState = variable().apply { if (available) lower(0).upper(1) else level(0) }

    val slots by lazy {
        Slot.all.filter { it.block == this }
    }

    fun addConstraints() {
        if (available) {
            addExpression().lower(0).upper(1).apply {
                ScheduledClass.all.asSequence().flatMap { it.anchorOverlapFor(this@Block) }
                        .filter { it.block.available }
                        .forEach {
                            set(it.occupied, 1)
                        }
            }
        } else {
            addExpression().level(0).apply {
                ScheduledClass.all.asSequence().flatMap { it.anchorOverlapFor(this@Block) }
                        .filter { it.block.available }
                        .forEach {
                            set(it.occupied, 1)
                        }
            }
        }
    }

    companion object {

        // Operating blocks
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

    val repetitionGapSlots = repetitionGapDays * 24 * 4

    val slotsNeeded = (hoursLength * 4).toInt()

    val slots by lazy {
        Slot.all.asSequence().filter { it.session == this }.toList()
    }

    val batches by lazy {
        slots.rollingRecurrences(slotsNeeded = slotsNeeded, gapSize = repetitionGapSlots, recurrencesNeeded = repetitions)
    }

    fun anchorOverlapFor(block: Block) = batches.asSequence()
            .filter { it.flatMap { it }.any { it.block == block } }
            .map { it.first().first() }

    val start get() = slots.asSequence().filter { it.occupied.value.toInt() == 1 }.map { it.block.dateTimeRange.start }.min()!!
    val end get() = start.plusMinutes((hoursLength * 60.0).toLong())

    val daysOfWeek get() = (0..(repetitions-1)).asSequence().map { start.dayOfWeek.plus(it.toLong() * repetitionGapDays) }.sorted()

    fun addConstraints() {

        //sum of all boolean states for this session must be 1
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



data class Slot(val block: Block, val session: ScheduledClass) {
    val occupied = variable().apply { if (block.available) binary() else level(0) }

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

fun <T> List<T>.rollingRecurrences(slotsNeeded: Int, gapSize: Int, recurrencesNeeded: Int) =
        (0..size).asSequence().map { i ->
            (1..recurrencesNeeded).asSequence().map { (it - 1) * gapSize  }
                    .filter { it + i < size}
                    .map { r ->
                        subList(i + r, (i + r + slotsNeeded).let { if (it > size) size else it })
                    }.filter { it.size == slotsNeeded }
                    .toList()
        }.filter { it.size == recurrencesNeeded }