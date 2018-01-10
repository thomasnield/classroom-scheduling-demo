# Schedule Generator Demo
### Using Linear/Integer Optimization

This was an exercise at creating a class schedule generator using [linear/integer optimization](https://en.wikipedia.org/wiki/Linear_programming). I finally got it working and I will blog about it later :D

I used [Kotlin](http://kotlinlang.org/) with [ojAlgo](http://www.ojalgo.org/), which turned out to be an effective stack.

For now, the model just assumes we are scheduling against one room.


## Data Input

Here is the starting data set:

**Classrooms:**

1) Psych 101 (1 hour, 2 sessions/week)
2) English 101 (1.5 hours, 2 sessions/week)
3) Math 300 (1.5 hours, 2 sessions/week)
4) Psych 300 (3 hours, 1 session/week)
5) Calculus I (2 hours, 2 sessions/week)
6) Linear Algebra I (2 hours, 3 sessions/week)
7) Sociology 101 (1 hour, 2 sessions/week)
8) Biology 101 (1 hour, 2 sessions/week)

I set the model to put each recurring session 48 hours apart.

**Availability for each day Monday-Friday:**

* 8:00AM-11:30AM
* 1:00PM-5:00PM

## Output

```
Psych 101- WEDNESDAY/FRIDAY 16:15-17:15
English 101- MONDAY/WEDNESDAY/FRIDAY 10:00-11:30
Math 300- TUESDAY/THURSDAY 10:00-11:30
Psych 300- THURSDAY 14:15-17:15
Calculus I- TUESDAY/THURSDAY 08:00-10:00
Linear Algebra I- MONDAY/WEDNESDAY/FRIDAY 13:15-15:15
Sociology 101- MONDAY/WEDNESDAY 15:15-16:15
Biology 101- WEDNESDAY/FRIDAY 08:45-09:45
```

![](https://i.imgur.com/uekAZBG.png)

Obviously, a room cannot be occupied by more than one class at any time. The solver does this successfully and prevents any overlap in scheduling. 


## How to Execute

Build the Kotlin project with Gradle, then run the `main()` function inside the `InputAndRun.kt` file. You can also change the hardcoded inputs in that file too.

This can take 30-60 seconds to run depending on your machine's computing power. I learned I could put a few functions in the model to gently guide it away from unncessary territory. For instance, classes with 3 repetitions a week need to have the first session on Monday. There's no reason to have it explore any other permutations later in the week. 

## Observations

I studied linear/integer programming for the past few months with both Python and Java libraries. One thing I noticed quickly is a lot of libraries implement models as a sea of numbers, which can be difficult to debug, refactor, and comprehend. By using Kotlin and ojAlgo, I was able to create a highly organized model that is easy to comprehend and evolve by utilizing classes, fluent functional pipelines, and the Java/Kotlin standard library.

The Java 8 Date/Time library was immensely helpful to turn 15-minute time increments into discrete integer intervals. The domain classes in my model were designed to provide both the `LocalDateTime` and discrete integer representations of time variables, making it friendly for both the model and developer.

It was satisfying that Kotlin allowed me to create something procedural and hacky as I worked through my thought process, and yet I could safely extract out DSL's, functions, and API's later. This is something that should not be taken for granted, as a common workflow for many data science teams is to "hack up" something in R or Python, only to have it rewritten from scratch later in Java. For this reason, a lot of models stay on data scientists' laptops and never see production.

 With a lot of Python, R, and even Java libraries, it is easy to be constrained by the maintainability of the code. Thankfully, with this stack I spent far more time with a pencil and paper trying to figure out the algebraic linear functions. Once I had that figured out, execution was easy. Problems only surfaced when I did something conceptually wrong with my math.

## Roadmap

* [x] Optimize non-overlap binary constraints for discrete values, not linear

* [x] Space out recurring classes by one day

* [x] Find performance bottlenecks

* [x] Constrain scheduling to only available times above

* [ ] Investigate 15 minute spills past boundaries

* [ ] Wrap TornadoFX user interface around model

* [ ] Support multiple rooms

* [ ] Explore [OptaPlanner](https://docs.optaplanner.org/7.4.1.Final/optaplanner-docs/html_single/index.html) implementation 

