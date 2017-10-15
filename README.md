# Class Scheduling Algorithm Demo

I want to create (or welcome contributions or forks) to develop a solution for a simple scenario: scheduling university classes across a limited set of classrooms over a week. I'd strongly prefer a mathematical approach using linear algebra techniques rather than a brute-force approach. 

This is purely an exercise for my own and others' learning.  

Preferred languages are Python, Java, or Kotlin. Please use only lightweight libraries like Numpy or ND4J.

Here is a starting data set: 

**Classrooms:**

1) Psych 101 (1 hour, 2 sessions/week)
2) English 101 (1.5 hours, 2 sessions/week)
3) Math 300 (1.5 hours, 2 sessions/week)
4) Psych 300 (3 hours, 1 session/week)
5) Calculus I (2 hours, 3 sessions/week)
6) Calculus II (2 hours, 3 sessions/week)
7) Sociology 101 (1 hour, 2 sessions/week)
8) Sociology 102 (1 hour, 2 sessions/week)


**Rooms:**

* ROOM A
* ROOM B
* ROOM C

**Availability for each day Monday-Friday:**

* 8:00AM-11:30AM
* 1:00PM-5:00PM


Obviously, a room cannot be occupied by more than one class at any time. If the solver cannot find a solution, it should raise an error indicating no solution exists. 

If this initial iteration gets solved, I may add more constraints. 
