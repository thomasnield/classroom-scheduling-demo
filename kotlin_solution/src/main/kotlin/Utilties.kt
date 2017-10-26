import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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