# ADR 0002: Modular monolith for the first release

- Status: Accepted
- Decision: Deliver one Spring Boot server with separate Maven modules for the domain model, simulation engine, and behaviour profiles.
- Rationale: Keeps the first release approachable and avoids distributed coordination before it is required.
- Consequences: Bound execution concurrency in the server. A separate runner service can be introduced later behind the engine API.
