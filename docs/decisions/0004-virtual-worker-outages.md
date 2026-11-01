# ADR 0004: Model worker outages as capacity reductions

- Status: Accepted
- Decision: A planned outage removes a configured number of worker slots from new dispatch for a virtual-time interval; active jobs continue to their scheduled completion.
- Rationale: This creates a deterministic first model of reduced worker capacity without pretending to model process termination or distributed lock recovery.
- Consequences: This is not a stalled-job or heartbeat-loss simulation. Active-job interruption and recovery require a later worker-identity and lease model.
