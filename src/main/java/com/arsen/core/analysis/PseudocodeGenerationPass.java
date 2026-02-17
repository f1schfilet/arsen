package com.arsen.core.analysis;

import com.arsen.core.event.Event;
import com.arsen.core.event.EventBus;
import com.arsen.core.event.EventType;
import com.arsen.core.pseudocode.PseudocodeService;
import com.arsen.model.disassembly.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PseudocodeGenerationPass implements AnalysisPass {
    private final PseudocodeService pseudocodeService;
    private final EventBus eventBus;

    public PseudocodeGenerationPass() {
        this.pseudocodeService = PseudocodeService.getInstance();
        this.eventBus = EventBus.getInstance();
    }

    @Override
    public String getName() {
        return "Pseudocode Generation";
    }

    @Override
    public void execute(AnalysisContext context) {
        log.debug("Generating pseudocode for {} functions", context.getFunctions().size());

        int count = 0;
        for (Function function : context.getFunctions().values()) {
            try {
                String pseudocode = pseudocodeService.generatePseudocode(function);
                if (pseudocode != null && !pseudocode.trim().isEmpty()) {
                    count++;
                    log.trace("Generated pseudocode for {}: {} lines", function.getName(), pseudocode.split("\n").length);
                } else {
                    log.warn("Empty pseudocode generated for function: {}", function.getName());
                }
            } catch (Exception e) {
                log.error("Failed to generate pseudocode for function: {}", function.getName(), e);
            }
        }

        log.info("Successfully generated pseudocode for {}/{} functions", count, context.getFunctions().size());
        eventBus.publish(Event.of(EventType.PSEUDOCODE_GENERATED, count));
    }
}
