package io.queueflux.server.service;

import io.queueflux.engine.api.*;
import io.queueflux.engine.api.SimulationEngine;
import io.queueflux.model.scenario.ScenarioDefinition;
import io.queueflux.model.scenario.ScenarioValidator;
import io.queueflux.server.api.SimulationRunSnapshot;
import io.queueflux.server.persistence.SimulationRunRepository;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SimulationRunService {
    private final SimulationEngine engine;
    private final SimulationRunRepository repository;
    private final TaskExecutor executor;
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SimulationRunService(SimulationEngine engine, SimulationRunRepository repository,
            @Qualifier("simulationTaskExecutor") TaskExecutor executor) {
        this.engine = engine;
        this.repository = repository;
        this.executor = executor;
    }

    public UUID submit(ScenarioDefinition scenario) {
        ScenarioValidator.validate(scenario);
        UUID id = UUID.randomUUID();
        repository.create(id, scenario);
        executor.execute(() -> execute(id, scenario));
        return id;
    }

    public Optional<SimulationRunSnapshot> find(UUID id) {
        return repository.find(id);
    }

    public SseEmitter subscribe(UUID id) {
        SseEmitter emitter = new SseEmitter(0L);
        CopyOnWriteArrayList<SseEmitter> listeners = subscribers.computeIfAbsent(id, ignored -> new CopyOnWriteArrayList<>());
        listeners.add(emitter);
        emitter.onCompletion(() -> remove(id, emitter));
        emitter.onTimeout(() -> remove(id, emitter));
        emitter.onError(ignored -> remove(id, emitter));
        repository.find(id).ifPresent(snapshot -> {
            send(emitter, "snapshot", snapshot);
            if (isTerminal(snapshot.status())) {
                remove(id, emitter);
                emitter.complete();
            }
        });
        return emitter;
    }

    private void execute(UUID id, ScenarioDefinition scenario) {
        try {
            repository.markRunning(id);
            publish(id, "status", Map.of("status", "RUNNING"));
            SimulationResult result = engine.run(SimulationRequest.defaults(scenario), progress -> {
                repository.updateProgress(id, progress);
                publish(id, "progress", progress);
            });
            repository.markCompleted(id, result);
            repository.find(id).ifPresent(snapshot -> publish(id, "completed", snapshot));
        } catch (RuntimeException exception) {
            repository.markFailed(id, exception.getMessage());
            repository.find(id).ifPresent(snapshot -> publish(id, "failed", snapshot));
        } finally {
            completeListeners(id);
        }
    }

    private void publish(UUID id, String event, Object payload) {
        CopyOnWriteArrayList<SseEmitter> listeners = subscribers.get(id);
        if (listeners == null) return;
        for (SseEmitter emitter : listeners) {
            try { send(emitter, event, payload); }
            catch (RuntimeException exception) { remove(id, emitter); }
        }
    }

    private void send(SseEmitter emitter, String event, Object payload) {
        try { emitter.send(SseEmitter.event().name(event).data(payload, MediaType.APPLICATION_JSON)); }
        catch (IOException exception) { throw new IllegalStateException("could not publish run event", exception); }
    }

    private void remove(UUID id, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> listeners = subscribers.get(id);
        if (listeners != null) {
            listeners.remove(emitter);
            if (listeners.isEmpty()) subscribers.remove(id, listeners);
        }
    }

    private void completeListeners(UUID id) {
        CopyOnWriteArrayList<SseEmitter> listeners = subscribers.remove(id);
        if (listeners != null) listeners.forEach(SseEmitter::complete);
    }

    private boolean isTerminal(String status) {
        return "COMPLETED".equals(status) || "FAILED".equals(status);
    }
}
