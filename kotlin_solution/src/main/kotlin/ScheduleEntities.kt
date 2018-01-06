import java.time.DayOfWeek
import java.time.LocalDateTime


data class Block(val dateTimeRange: ClosedRange<LocalDateTime>) {

    val timeRange = dateTimeRange.let { it.start.toLocalTime()..it.endInclusive.toLocalTime() }

    fun addConstraints() {
        val f = addExpression().upper(1)

        OccupationState.all.filter { it.block == this }.forEach {
            f.set(it.occupied, 1)
        }
    }
    companion object {

        // Operating blocks
        val all by lazy {
            generateSequence(operatingDates.start.atTime(operatingDay.start)) {
                it.plusMinutes(15).takeIf { it.plusMinutes(15) <= operatingDates.endInclusive.atTime(operatingDay.endInclusive) }
            }.map { Block(it..it.plusMinutes(15)) }
             .toList()
        }
    }
}


data class ScheduledClass(val id: Int,
                          val name: String,
                          val hoursLength: Double,
                          val repetitions: Int) {

    val sessions by lazy {
        Session.all.filter { it.parentClass == this }
    }

    fun addConstraints() {

        //guide 3 repetitions to be fixed on MONDAY, WEDNESDAY, FRIDAY
        if (repetitions == 3) {
            sessions.forEach { session ->
                val f = addExpression().level(session.blocksNeeded)

                session.occupationStates.asSequence()
                        .filter {
                            it.block.dateTimeRange.start.dayOfWeek ==
                                    when(session.repetitionIndex) {
                                        1 -> DayOfWeek.MONDAY
                                        2 -> DayOfWeek.WEDNESDAY
                                        3 -> DayOfWeek.FRIDAY
                                        else -> throw Exception("Must be 1/2/3")
                                    }
                        }
                        .forEach {
                            f.set(it.occupied,1)
                        }
            }
        }
    }

    companion object {
        val all by lazy { scheduledClasses }
    }
}


data class Session(val id: Int,
                   val name: String,
                   val hoursLength: Double,
                   val repetitionIndex: Int,
                   val parentClass: ScheduledClass) {

    val blocksNeeded = (hoursLength * 4).toInt()

    val occupationStates by lazy {
        OccupationState.all.asSequence().filter { it.session == this }.toList()
    }

    val start get() = occupationStates.asSequence().filter { it.occupied.value.toInt() == 1 }
            .map { it.block.dateTimeRange.start }
            .min()!!

    val end get() = occupationStates.asSequence().filter { it.occupied.value.toInt() == 1 }
            .map { it.block.dateTimeRange.endInclusive }
            .max()!!

    fun addConstraints() {

        val f1 = addExpression().level(0)
        //block out exceptions
        occupationStates.asSequence()
                .filter { os -> breaks.any { os.block.timeRange.start in it } || os.block.timeRange.start !in operatingDay }
                .forEach {
                    // b = 0, where b is occupation state
                    // this means it should never be occupied
                    f1.set(it.occupied, 1)
                }

        //sum of all boolean states for this session must equal the # blocks needed
        val f2 = addExpression().level(blocksNeeded)

        occupationStates.forEach {
            f2.set(it.occupied, 1)
        }
    }

    companion object {
        val all by lazy {
            ScheduledClass.all.asSequence().flatMap { sc ->
                (1..sc.repetitions).asSequence()
                        .map { Session(sc.id, sc.name, sc.hoursLength, it, sc) }
            }.toList()
        }
    }
}

data class OccupationState(val block: Block, val session: Session) {
    val occupied = variable().binary()

    companion object {

        val all by lazy {
            Block.all.asSequence().flatMap { b ->
                Session.all.asSequence().map { OccupationState(b,it) }
            }.toList()
        }
    }
}


fun applyConstraints() {
    Session.all.forEach { it.addConstraints() }
    ScheduledClass.all.forEach { it.addConstraints() }
    Block.all.forEach { it.addConstraints() }
}
