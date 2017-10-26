import java.time.LocalDateTime

data class AvailableRange(val dateTimeRange: ClosedRange<LocalDateTime>) {

    val discreteRange = dateTimeRange.asDiscrete()
}

data class ScheduledClass(val id: Int,
                          val name: String,
                          val hoursLength: Double,
                          val repetitions: Int) {

    val scheduledSessions by lazy {
        (1..repetitions).asSequence()
                .map { Session(id, name, hoursLength, it, this) }
                .toList()
    }
    fun addConstraints() = scheduledSessions.forEach { it.addConstraints() }
}


data class Session(val id: Int,
                   val name: String,
                   val hoursLength: Double,
                   val repetitionIndex: Int,
                   val parentClass: ScheduledClass) {

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
        parentClass.scheduledSessions
                .asSequence()
                .filter { it.repetitionIndex == repetitionIndex - 1 }
                .forEach { otherSession ->

                    model.addExpression()
                            .level(48 * 4)
                            .set(startDiscrete, 1)
                            .set(otherSession.startDiscrete, -1)
                }

        // don't overlap with other classes
        scheduledClasses.asSequence()
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

        // put at least 15 minutes between each class

        scheduledClasses.asSequence()
                .filter { it != this.parentClass }



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

