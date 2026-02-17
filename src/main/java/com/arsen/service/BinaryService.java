package com.arsen.service;

import com.arsen.core.analysis.AnalysisEngine;
import com.arsen.core.analysis.AnalysisResult;
import com.arsen.core.event.Event;
import com.arsen.core.event.EventBus;
import com.arsen.core.event.EventType;
import com.arsen.loader.BinaryLoader;
import com.arsen.loader.BinaryLoaderFactory;
import com.arsen.model.binary.BinaryFile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class BinaryService {
    private final AnalysisEngine analysisEngine;
    private final EventBus eventBus;
    @Getter
    private BinaryFile currentBinary;
    @Getter
    private AnalysisResult currentAnalysis;

    public BinaryService() {
        this.analysisEngine = new AnalysisEngine();
        this.eventBus = EventBus.getInstance();
    }

    public CompletableFuture<BinaryFile> loadBinary(Path path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Loading binary file: {}", path);
                BinaryLoader loader = BinaryLoaderFactory.getLoader(path);
                BinaryFile binary = loader.load(path);
                this.currentBinary = binary;
                eventBus.publish(Event.of(EventType.BINARY_LOADED, binary));
                return binary;
            } catch (Exception e) {
                log.error("Failed to load binary", e);
                eventBus.publish(Event.of(EventType.ERROR_OCCURRED, e.getMessage()));
                throw new RuntimeException("Failed to load binary: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<AnalysisResult> analyze() {
        if (currentBinary == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No binary loaded"));
        }

        return analysisEngine.analyze(currentBinary).thenApply(result -> {
            this.currentAnalysis = result;
            return result;
        });
    }

    public void shutdown() {
        analysisEngine.shutdown();
    }
}
