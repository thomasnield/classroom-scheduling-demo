
import java.time.LocalDateTime


/** A discrete, 15-minute chunk of time a class can be scheduled on */
data class Block(val dateTimeRange: ClosedRange<LocalDateTime>) {

    val timeRange = dateTimeRange.let { it.start.toLocalTime()..it.endInclusive.toLocalTime() }

    /** indicates if this block is zeroed due to operating day/break constraints */
    val withinOperatingDay get() =  breaks.all { timeRange.start !in it } &&
            timeRange.start in operatingDay &&
            timeRange.endInclusive in operatingDay

    val affectingSlots get() = ScheduledClass.all.asSequence().flatMap { it.affectingSlotsFor(this) }

    companion object {

        /* All operating blocks for the entire week, broken up in 15 minute increments */
        val all by lazy {
            generateSequence(operatingDates.start.atStartOfDay()) {
                it.plusMinutes(15).takeIf { it.plusMinutes(15) <= operatingDates.endInclusive.atTime(23,59) }
            }.map { Block(it..it.plusMinutes(15)) }
             .toList()
        }
    }
}


data class ScheduledClass(val id: Int,
                          val name: String,
                          val hoursLength: Double,
                          val recurrences: Int,
                          val repetitionGapDays: Int = 2) {

    /** the # of slots between each recurrence */
    val gap = repetitionGapDays * 24 * 4

    /** the # of slots needed for a given occurrence */
    val slotsNeeded = (hoursLength * 4).toInt()

    /** yields slots for this given scheduled class */
    val slots by lazy {
        Slot.all.asSequence().filter { it.scheduledClass == this }.toList()
    }

    /** yields slot groups for this scheduled class */
    val slotGroups by lazy {
        slots.rollingRecurrences(slotsNeeded = slotsNeeded, gap = gap, recurrences = recurrences)
    }

    /** yields slots that affect the given block for this scheduled class */
    fun affectingSlotsFor(block: Block) = slotGroups.asSequence()
            .filter { it.flatMap { it }.any { it.block == block } }
            .map { it.first().first() }

    /** These slots should be fixed to zero **/
    val slotsFixedToZero by lazy {
        // broken recurrences
        slots.rollingRecurrences(slotsNeeded, gap, recurrences, RecurrenceMode.PARTIAL_ONLY)
                .asSequence()
                .flatMap { it.asSequence() }
                .flatMap { it.asSequence() }
                // operating day blackouts
                .plus(slots.asSequence().filter { !it.block.withinOperatingDay })
                // affected slots that occupy blackouts
                .plus(
                        slotGroups.asSequence()
                                .filter { it.any { it.any { !it.block.withinOperatingDay }} }
                                .flatMap { it.asSequence() }
                                .flatMap { it.asSequence() }
                )
                .distinct()
                .onEach { it.selected = 0 }
                .toList()
    }

    /** translates and returns the optimized start time of the class */
    val start get() = slots.asSequence().filter { it.selected == 1 }.map { it.block.dateTimeRange.start }.min()!!

    /** translates and returns the optimized end time of the class */
    val end get() = start.plusMinutes((hoursLength * 60.0).toLong())

    /** returns the DayOfWeeks where recurrences take place */
    val daysOfWeek get() = (0..(recurrences-1)).asSequence().map { start.dayOfWeek.plus(it.toLong() * repetitionGapDays) }.sorted()

    companion object {
        val all by lazy { scheduledClasses }
    }
}



data class Slot(val block: Block, val scheduledClass: ScheduledClass) {

    var selected: Int? = null

    companion object {

        val all by lazy {
            Block.all.asSequence().flatMap { b ->
                ScheduledClass.all.asSequence().map { Slot(b,it) }
            }.toList()
        }
    }
}


fun <T> List<T>.rollingBatches(batchSize: Int) = (0..size).asSequence().map { i ->
    subList(i, (i + batchSize).let { if (it > size) size else it })
}.filter { it.size == batchSize }

enum class RecurrenceMode { PARTIAL_ONLY, FULL_ONLY, ALL }

fun <T> List<T>.rollingRecurrences(slotsNeeded: Int, gap: Int, recurrences: Int, mode: RecurrenceMode = RecurrenceMode.FULL_ONLY) =
        (0..size).asSequence().map { i ->
            (1..recurrences).asSequence().map { (it - 1) * gap }
                    .filter { it + i < size}
                    .map { r ->
                        subList(i + r, (i + r + slotsNeeded).let { if (it > size) size else it })
                    }
                    .toList()
        }.filter {
            when (mode) {
                RecurrenceMode.ALL -> true
                RecurrenceMode.FULL_ONLY -> it.size == recurrences && it.all { it.size == slotsNeeded }
                RecurrenceMode.PARTIAL_ONLY -> it.size < recurrences || it.any { it.size < slotsNeeded }
            }
        }
