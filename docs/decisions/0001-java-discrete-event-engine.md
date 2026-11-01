# ADR 0001: Java discrete-event simulation core

- Status: Accepted
- Decision: Implement the simulation engine in plain Java, independent of Spring Boot, using integer-microsecond virtual time and a priority queue of events.
- Rationale: It is deterministic, fast to test, easy to embed in Spring Boot, and reusable as a library.
- Consequences: Engine behaviour must be defined in event-ordering contracts; real-time side effects and arbitrary user code are out of scope.
