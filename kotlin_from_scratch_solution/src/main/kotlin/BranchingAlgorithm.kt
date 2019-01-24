import java.time.DayOfWeek

class BranchNode(val selectedValue: Int,
                 restOfTree: List<Slot>,
                 val previous: BranchNode? = null) {

    val slot = restOfTree.first()

    val traverseBackwards =  generateSequence(this) { it.previous }.toList()

    // calculate remaining slots and prune where constraint propagates
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
        }
    }


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

fun executeBranchingSearch() {

    // pre-constraints
    ScheduledClass.all.flatMap { it.slotsFixedToZero }.forEach { it.selected = 0 }

    // Try to encourage most "constrained" slots to be evaluated first
    val sortedSlots = Slot.all.asSequence().filter { it.selected == null }.sortedWith(
            compareBy(
                    {
                        // prioritize slots dealing with recurrences
                        val dow = it.block.range.start.dayOfWeek
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
                    { it.block.range.start }, // make search start at beginning of week
                    {-it.scheduledClass.slotsNeededPerSession } // followed by class length,

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