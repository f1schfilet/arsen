package com.arsen.core.analysis;

import com.arsen.core.event.Event;
import com.arsen.core.event.EventBus;
import com.arsen.core.event.EventType;
import com.arsen.model.binary.BinaryFile;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class AnalysisEngine {
    private final ExecutorService executor;
    private final List<AnalysisPass> analysisPasses;
    private final EventBus eventBus;

    public AnalysisEngine() {
        this.executor = Executors.newFixedThreadPool(4);
        this.analysisPasses = new CopyOnWriteArrayList<>();
        this.eventBus = EventBus.getInstance();
        registerDefaultPasses();
    }

    private void registerDefaultPasses() {
        analysisPasses.add(new FunctionDetectionPass());
        analysisPasses.add(new ControlFlowAnalysisPass());
        analysisPasses.add(new CrossReferencePass());
        analysisPasses.add(new StringAnalysisPass());
        analysisPasses.add(new PseudocodeGenerationPass());
    }

    public CompletableFuture<AnalysisResult> analyze(BinaryFile binaryFile) {
        log.info("Starting analysis of binary: {}", binaryFile.getFilePath());
        eventBus.publish(Event.of(EventType.ANALYSIS_STARTED, binaryFile));

        return CompletableFuture.supplyAsync(() -> {
            AnalysisContext context = new AnalysisContext(binaryFile);

            for (int i = 0; i < analysisPasses.size(); i++) {
                AnalysisPass pass = analysisPasses.get(i);
                log.debug("Executing analysis pass: {}", pass.getName());

                try {
                    pass.execute(context);
                    int progress = (int) (((i + 1.0) / analysisPasses.size()) * 100);
                    eventBus.publish(Event.of(EventType.ANALYSIS_PROGRESS, progress));
                } catch (Exception e) {
                    log.error("Error in analysis pass: {}", pass.getName(), e);
                }
            }

            AnalysisResult result = context.buildResult();
            eventBus.publish(Event.of(EventType.ANALYSIS_COMPLETED, result));
            log.info("Analysis completed successfully");
            return result;
        }, executor);
    }

    public void registerPass(AnalysisPass pass) {
        analysisPasses.add(pass);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
