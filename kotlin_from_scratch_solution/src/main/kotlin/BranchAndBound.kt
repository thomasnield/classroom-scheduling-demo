

class BranchNode(val selectedValue: Int, val slot: Slot, val previous: BranchNode? = null) {

    val traverseBackwards =  generateSequence(this) { it.previous }.toList()

    val noConflictOnClass = traverseBackwards.asSequence()
            .filter { it.slot.scheduledClass == slot.scheduledClass }
            .map { it.selectedValue }
            .sum() <= 1

    val slotAffectingNodes = slot.block.affectingSlots.toSet().let { affectSlots ->
        traverseBackwards.asSequence().filter { it.slot in affectSlots }
    }.toList()

    val noConflictOnBlock = slotAffectingNodes.asSequence()
            .map { it.selectedValue }
            .sum() <= 1

    val noConflictOnFixed = !(selectedValue == 1 && slot in slot.scheduledClass.slotsFixedToZero)

    val constraintsMet = noConflictOnClass && noConflictOnBlock && noConflictOnFixed

    val scheduleMet = traverseBackwards.asSequence()
            .filter { it.selectedValue == 1 }
            .map { it.slot.scheduledClass }
            .distinct()
            .count() == ScheduledClass.all.count()

    val isContinuable = constraintsMet && traverseBackwards.count() < Slot.all.count()
    val isSolution = scheduleMet && constraintsMet

    fun applySolution() {
        slot.selected = selectedValue
    }
}

fun executeBranchAndBound() {

    // pre-constraints
    ScheduledClass.all.flatMap { it.slotsFixedToZero }.forEach { it.selected = 0 }

    // To avoid exhaustive search, it is critical to start with slots having fixed values, then the most "constrained" slots
    // Finding the right heuristic to define "constrained" is the key part
    val sortedByMostConstrained = Slot.all.sortedWith(
            compareBy(
                    { it.selected?:1000 }, // fixed values go first, solvable values go last
                    {it.block.dateTimeRange.start}, // encourage search to start at beginning of week
                    {-it.scheduledClass.recurrences} // and assign high-recurrence classes first
            )
    )

    // this is a recursive function for exploring nodes in a branch-and-bound tree
    fun traverse(index: Int, currentBranch: BranchNode): BranchNode? {

        val nextSlot = sortedByMostConstrained[index+1]

        // we want to explore possible values 0..1 unless this cell is fixed already
        val fixedValue = nextSlot.selected

        // selecting 1 first is an absolute must so it always attempts to put in a class before not putting it
        val range = if (fixedValue == null) intArrayOf(1,0) else intArrayOf(fixedValue)

        for (candidateValue in range) {

            val nextBranch = BranchNode(candidateValue, nextSlot, currentBranch)

            if (nextBranch.isSolution)
                return nextBranch

            if (nextBranch.isContinuable) {
                val terminalBranch = traverse(index + 1, nextBranch)
                if (terminalBranch?.isSolution == true) {
                    return terminalBranch
                }
            }
        }
        return null
    }


    // start with the first Slot and set it as the seed
    val seed = sortedByMostConstrained.first()
            .let { BranchNode(it.selected?:throw Exception("There are no fixed values?! Seriously?"), it) }

    // recursively traverse from the seed and get a solution
    val solution = traverse(0, seed)

    solution?.traverseBackwards?.forEach { it.applySolution() }?: throw Exception("Infeasible")
}