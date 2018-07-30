import java.time.LocalDate
import java.time.LocalTime


// Any Monday through Friday date range will work
val operatingDates = LocalDate.of(2017,10,16)..LocalDate.of(2017,10,20)
val operatingDay = LocalTime.of(8,0)..LocalTime.of(17,0)


val breaks = listOf<ClosedRange<LocalTime>>(
        LocalTime.of(11,30)..LocalTime.of(12,59)
)


// classes
val scheduledClasses = listOf(
        ScheduledClass(id=1, name="Psych 101",hoursLength=1.0, recurrences=2),
        ScheduledClass(id=2, name="English 101", hoursLength=1.5, recurrences=3),
        ScheduledClass(id=3, name="Math 300", hoursLength=1.5, recurrences=2),
        ScheduledClass(id=4, name="Psych 300", hoursLength=3.0, recurrences=1),
        ScheduledClass(id=5, name="Calculus I", hoursLength=2.0, recurrences=2),
        ScheduledClass(id=6, name="Linear Algebra I", hoursLength=2.0, recurrences=3),
        ScheduledClass(id=7, name="Sociology 101", hoursLength=1.0, recurrences=2),
        ScheduledClass(id=8, name="Biology 101", hoursLength=1.0, recurrences=2)/*,
        ScheduledClass(id=9, name="Supply Chain 300", hoursLength=2.5, recurrences=2),
        ScheduledClass(id=10, name="Orientation 101",hoursLength=1.0, recurrences=1),

        // TODO ojAlgo says this is feasible and finds solution more quickly
        // We need to continuously check for feasibility as well in terms of classes being schedulable still at a given branch node
        ScheduledClass(id=11, name="Geography 300", hoursLength=3.0, recurrences=1)*/
        )

fun main(args: Array<String>) {

    println("Job started at ${LocalTime.now()}\r\n")

    executeBranchAndBound()

    ScheduledClass.all.forEach {
        println("${it.name}- ${it.daysOfWeek.joinToString("/")} ${it.start.toLocalTime()}-${it.end.toLocalTime()}")
    }

    println("\r\nJob ended at ${LocalTime.now()}")

}