import java.time.DayOfWeek

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

    val recurrencesStillPossible = when {

        // If we reach past MONDAY in our search, we better have all of our 3-recurrences already scheduled on MONDAY
        slot.selected == null && slot.block.dateTimeRange.start.dayOfWeek > DayOfWeek.MONDAY  ->
                traverseBackwards.asSequence()
                        .filter { it.slot.scheduledClass.recurrences == 3 }
                        .groupBy { it.slot.scheduledClass }
                        .all { (_,grp) -> grp.any { it.selectedValue == 1 } }

        // If we reach past WEDNESDAY in our search, we better have all of our 2-recurrences already scheduled inside MONDAY through WEDNESDAY
        slot.selected == null && slot.block.dateTimeRange.start.dayOfWeek > DayOfWeek.WEDNESDAY ->
            traverseBackwards.asSequence()
                    .filter { it.slot.scheduledClass.recurrences == 2 }
                    .groupBy { it.slot.scheduledClass }
                    .all { (_,grp) -> grp.any { it.selectedValue == 1 } }

        else -> true
    }

    val scheduleMet = traverseBackwards.asSequence()
            .filter { it.selectedValue == 1 }
            .map { it.slot.scheduledClass }
            .distinct()
            .count() == ScheduledClass.all.count()

    val isContinuable = constraintsMet && recurrencesStillPossible && traverseBackwards.count() < Slot.all.count()
    val isSolution = scheduleMet && constraintsMet

    fun applySolution() {
        slot.selected = selectedValue
    }
}

fun executeBranchAndBound() {

    // pre-constraints
    ScheduledClass.all.flatMap { it.slotsFixedToZero }.forEach { it.selected = 0 }

    // To avoid exhaustive search, it is critical to sort solve variables on the correct heuristic
    // First sort on slots having fixed values being first, followed by the most "constrained" slots
    val sortedByMostConstrained = Slot.all.sortedWith(
            compareBy(
                    { it.selected?:1000 }, // fixed values go first, solvable values go last
                    {
                        // prioritize slots dealing with recurrences
                        val dow = it.block.dateTimeRange.start.dayOfWeek
                        when {
                            dow == DayOfWeek.MONDAY && it.scheduledClass.recurrences == 3 -> -1000
                            dow <= DayOfWeek.WEDNESDAY && it.scheduledClass.recurrences == 2 -> -500
                            else -> 0
                        }
                    }, // encourage search to start at beginning of week
                    {-it.scheduledClass.slotsNeededPerSession } // followed by class length
            )
    )
//TODO this case is broken
    /*
    Psych 101- TUESDAY/THURSDAY 10:00-11:00
    English 101- MONDAY/WEDNESDAY/FRIDAY 10:00-11:30
    Math 300- MONDAY/WEDNESDAY 15:30-17:00
    Psych 300- THURSDAY 14:00-17:00 *CONFLICT*
    Calculus I- TUESDAY/THURSDAY 08:00-10:00
    Linear Algebra I- MONDAY/WEDNESDAY/FRIDAY 08:00-10:00
    Sociology 101- TUESDAY/THURSDAY 13:00-14:00
    Biology 101- TUESDAY/THURSDAY 14:15-15:15 *CONFLICT*
    Supply Chain 300- MONDAY/WEDNESDAY 13:00-15:30
    Orientation 101- TUESDAY 14:00-15:00 *CONFLICT*
    Geography 300- FRIDAY 13:00-16:00
     */
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