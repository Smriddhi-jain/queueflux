package io.queueflux.server.api;

import io.queueflux.engine.api.SimulationRequest;
import io.queueflux.engine.api.SimulationResult;
import io.queueflux.engine.api.SimulationEngine;
import io.queueflux.model.scenario.ScenarioDefinition;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simulations")
public class SimulationController {
    private final SimulationEngine engine;

    public SimulationController(SimulationEngine engine) { this.engine = engine; }

    @PostMapping("/run")
    public ResponseEntity<SimulationResult> run(@Valid @RequestBody ScenarioDefinition scenario) {
        return ResponseEntity.ok(engine.run(SimulationRequest.defaults(scenario)));
    }
}
