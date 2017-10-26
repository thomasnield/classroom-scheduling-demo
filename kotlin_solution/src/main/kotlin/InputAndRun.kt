import java.time.LocalDate
import java.time.LocalTime


// Any Monday through Friday date range will work
val operatingDates = LocalDate.of(2017,10,16)..LocalDate.of(2017,10,20)

val operatingTimes = listOf(
        LocalTime.of(8,0)..LocalTime.of(11,30),
        LocalTime.of(13,0)..LocalTime.of(17,0)
)


// classes
val scheduledClasses = listOf(
        ScheduledClass(id=1, name="Psych 101", hoursLength=1.0, repetitions=2),
        ScheduledClass(id=2, name="English 101", hoursLength=1.5, repetitions=2),
        ScheduledClass(id=3, name="Math 300", hoursLength=1.5, repetitions=2),
        ScheduledClass(id=4, name="Psych 300", hoursLength=3.0, repetitions=1),
        ScheduledClass(id=5, name="Calculus I", hoursLength=2.0, repetitions=2),
        ScheduledClass(id=6, name="Linear Algebra I", hoursLength=2.0, repetitions=3),
        ScheduledClass(id=7, name="Sociology 101", hoursLength=1.0, repetitions=2),
        ScheduledClass(id=8, name="Biology 101", hoursLength=1.0, repetitions=2)
)


fun main(args: Array<String>) {

    scheduledClasses.forEach { it.addConstraints() }
    addModelHelpers()

    println(model.minimise())

    scheduledClasses.asSequence()
            .flatMap { it.scheduledSessions.asSequence() }
            .sortedBy { it.dateTimeRange.first }.forEach {
                println("${it.name}-${it.repetitionIndex} ${it.date.dayOfWeek} ${it.timeRange}")
            }
}