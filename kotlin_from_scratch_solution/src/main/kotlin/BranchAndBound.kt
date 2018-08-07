import java.time.DayOfWeek
import java.time.LocalDateTime

//TODO dynamically sort a prioritized list for each BranchNode that next explores affected regions for a booked slot
class BranchNode(val selectedValue: Int,
                 restOfTree: List<Slot>,
                 val previous: BranchNode? = null) {

    val slot = restOfTree.first()

    // calculate remaining slots and reprioritize
    val remainingSlots by lazy {
        if (selectedValue == 0)
            restOfTree.drop(1)
        else
            restOfTree.asSequence()
                    .drop(1)
                    .filter { it.scheduledClass != slot.scheduledClass }
                    .sortedBy { if (slot in it.block.affectingSlots) it.block.dateTimeRange.start else LocalDateTime.MAX }
                    .toList()
    }

    val traverseBackwards =  generateSequence(this) { it.previous }.toList()

    val selectedOnlyTraverseBackwards by lazy { traverseBackwards.filter { it.selectedValue == 1 } }

    val noConflictOnClass get() = if (selectedValue == 0) true else
        selectedOnlyTraverseBackwards
            .asSequence()
            .filter { it.slot.scheduledClass == slot.scheduledClass }
             .take(2).count() <= 1

    val slotAffectingNodes by lazy { traverseBackwards.asSequence().filter { it.slot in slot.block.affectingSlots }.toList() }

    // search backwards
    val noConflictOnBlock get() =  if (selectedValue == 0) true
    else slotAffectingNodes.asSequence()
            .map { it.selectedValue }
            .sum() <= 1


    val noRecurrenceOverlaps get() = Block.allInOperatingDay.asSequence()
            .all { block ->
                selectedOnlyTraverseBackwards
                        .asSequence()
                        .filter { it.slot in  block.affectingSlots }
                        .take(2).count() <= 1
            }

    val noConflictOnFixed get() = selectedValue == 0 || slot !in slot.scheduledClass.slotsFixedToZero

    val constraintsMet get() = if (selectedValue == 0) true else noConflictOnFixed && noConflictOnClass && noConflictOnBlock


    val scheduleMet get() = selectedOnlyTraverseBackwards
            .asSequence()
            .map { it.slot.scheduledClass }
            .distinct()
            .count() == ScheduledClass.all.count()

    val isContinuable get() = constraintsMet &&  remainingSlots.count() > 0
    val isSolution get() = (scheduleMet && constraintsMet && noRecurrenceOverlaps && remainingSlots.count() == 0)/*.also {
        println("$scheduleMet && $constraintsMet && $noRecurrenceOverlaps && ${remainingSlots.count()}")
    }
*/
    fun applySolution() {
        slot.selected = selectedValue
    }
}

fun executeBranchAndBound() {

    // pre-constraints
    ScheduledClass.all.flatMap { it.slotsFixedToZero }.forEach { it.selected = 0 }

    // To avoid exhaustive search, it is critical to sort solve variables on the correct heuristic
    // First sort on slots having fixed values being first, followed by the most "constrained" slots
    val sortedByMostConstrained = Slot.all.asSequence().filter { it.selected == null }.sortedWith(
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
/*
        val nextSlot = currentBranch?.remainingSlots?.first() ?: sortedByMostConstrained.first()

        // we want to explore possible values 0..1 unless this cell is fixed already
        val fixedValue = nextSlot.selected

        // selecting 1 first is an absolute must so it always attempts to put in a class before not putting it
        val range = if (fixedValue == null) intArrayOf(1,0) else intArrayOf(fixedValue)
*/

        for (candidateValue in intArrayOf(1,0)) {
            val nextBranch = BranchNode(candidateValue, currentBranch?.remainingSlots?: sortedByMostConstrained)

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