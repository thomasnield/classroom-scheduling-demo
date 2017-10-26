import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable
import java.util.concurrent.atomic.AtomicInteger

// declare model
val model = ExpressionsBasedModel()

val funcId = AtomicInteger(0)
val variableId = AtomicInteger(0)
fun variable() = Variable(variableId.incrementAndGet().toString().let { "Variable$it" }).apply(model::addVariable)
fun ExpressionsBasedModel.addExpression() = funcId.incrementAndGet().let { "Func$it"}.let { model.addExpression(it) }


fun addModelHelpers() {

    // First session of classes with three repetitions should start on Monday
    scheduledClasses.asSequence()
            .filter { it.repetitions == 3 }
            .flatMap { it.scheduledSessions.asSequence() }
            .filter { it.repetitionIndex == 1 }
            .forEach {
                model.addExpression()
                        .lower(availableBlocks[0].discreteRange.start)
                        .upper(availableBlocks[1].discreteRange.endInclusive)
                        .set(it.startDiscrete, 1)

                model.addExpression()
                        .lower(availableBlocks[0].discreteRange.start)
                        .upper(availableBlocks[1].discreteRange.endInclusive)
                        .set(it.endDiscrete, 1)
            }

    // Make at least 3 classes with two repetitions start on Tuesday
    scheduledClasses.asSequence()
            .filter { it.repetitions == 2 }
            .take(3)
            .flatMap { it.scheduledSessions.asSequence() }
            .filter { it.repetitionIndex == 1 }
            .forEach {
                model.addExpression()
                        .lower(availableBlocks[2].discreteRange.start)
                        .upper(availableBlocks[3].discreteRange.endInclusive)
                        .set(it.startDiscrete, 1)

                model.addExpression()
                        .lower(availableBlocks[2].discreteRange.start)
                        .upper(availableBlocks[3].discreteRange.endInclusive)
                        .set(it.endDiscrete, 1)
            }
}