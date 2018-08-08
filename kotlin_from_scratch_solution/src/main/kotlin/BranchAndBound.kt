import java.time.DayOfWeek

//TODO dynamically sort a prioritized list for each BranchNode that next explores affected regions for a booked slot
class BranchNode(val selectedValue: Int,
                 restOfTree: List<Slot>,
                 val previous: BranchNode? = null) {

    val slot = restOfTree.first()

    // calculate remaining slots and reprioritize
    val remainingSlots by lazy {
        if (selectedValue == 0)
            restOfTree.minus(slot)
        else {

            // if this slot is occupied, affected slots can be pruned
            val affectedSlotsPropogated = Block.allInOperatingDay.asSequence().filter {
                slot in it.affectingSlots
            }.flatMap { it.affectingSlots.asSequence() }
             .filter { it.selected == null }
             .toSet()

            restOfTree.asSequence()
                    .filter {
                        it.scheduledClass != slot.scheduledClass &&
                                it !in affectedSlotsPropogated
                    }.toList()
                    .also {
                        it
                    }
        }
    }

    val traverseBackwards =  generateSequence(this) { it.previous }.toList()

    val scheduleMet get() = traverseBackwards
            .asSequence()
            .filter { it.selectedValue == 1 }
            .map { it.slot.scheduledClass }
            .distinct()
            .count() == ScheduledClass.all.count()

    val isContinuable get() = !scheduleMet && remainingSlots.count() > 0
    val isSolution get() = scheduleMet

    fun applySolution() {
        slot.selected = selectedValue
    }
}

fun executeBranchAndBound() {

    // pre-constraints
    ScheduledClass.all.flatMap { it.slotsFixedToZero }.forEach { it.selected = 0 }

    // First sort on slots having fixed values being first, followed by the most "constrained" slots
    val sortedSlots = Slot.all.asSequence().filter { it.selected == null }.sortedWith(
            compareBy(
                    { it.selected?:1000 }, // fixed values go first, solvable values go last
                    {
                        // prioritize slots dealing with recurrences
                        val dow = it.block.dateTimeRange.start.dayOfWeek
                        when {
                            dow == DayOfWeek.MONDAY && it.scheduledClass.recurrences == 3 -> -1000
                            dow != DayOfWeek.MONDAY && it.scheduledClass.recurrences == 3 -> 1000
                            dow in DayOfWeek.MONDAY..DayOfWeek.WEDNESDAY && it.scheduledClass.recurrences == 2 -> -500
                            dow !in DayOfWeek.MONDAY..DayOfWeek.WEDNESDAY && it.scheduledClass.recurrences == 2 -> 500
                            dow in DayOfWeek.THURSDAY..DayOfWeek.FRIDAY && it.scheduledClass.recurrences == 1 -> -300
                            dow !in DayOfWeek.THURSDAY..DayOfWeek.FRIDAY && it.scheduledClass.recurrences == 1 -> 300
                            else -> 0
                        }
                    },
                    {-it.scheduledClass.slotsNeededPerSession }, // followed by class length,
                    { it.block.dateTimeRange.start.dayOfWeek } // make search start at beginning of week

            )
    ).toList()

    // this is a recursive function for exploring nodes in a branch-and-bound tree
    fun traverse(currentBranch: BranchNode? = null): BranchNode? {

        if (currentBranch != null && currentBranch.remainingSlots.isEmpty()) {
            return currentBranch
        }

        for (candidateValue in intArrayOf(1,0)) {
            val nextBranch = BranchNode(candidateValue, currentBranch?.remainingSlots?: sortedSlots, currentBranch)

            if (nextBranch.isSolution)
                return nextBranch

            if (nextBranch.isContinuable) {
                val terminalBranch = traverse(nextBranch)
                if (terminalBranch?.isSolution == true) {
                    return terminalBranch
                }
            }
        }
        return null
    }


    // start with the first Slot and set it as the seed
    // recursively traverse from the seed and get a solution
    val solution = traverse()

    solution?.traverseBackwards?.forEach { it.applySolution() }?: throw Exception("Infeasible")
}