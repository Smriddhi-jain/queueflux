# ADR 0003: Persist asynchronous run records in PostgreSQL

- Status: Accepted
- Decision: Use PostgreSQL JSONB for immutable input scenarios and final results, with relational columns for status, seed, timestamps, progress and errors.
- Rationale: A single relational store is simple to run locally with Docker Compose and supports durable run history without polluting the pure Java simulation engine.
- Consequences: The API requires a reachable PostgreSQL database. The first coordinator is process-local; a durable queue/lease protocol is still required for recovery and horizontal scaling.
