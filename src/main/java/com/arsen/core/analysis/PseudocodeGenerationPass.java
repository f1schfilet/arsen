package com.arsen.core.analysis;

import com.arsen.core.pseudocode.PseudocodeService;
import com.arsen.model.disassembly.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class PseudocodeGenerationPass implements AnalysisPass {
    private final PseudocodeService pseudocodeService;

    public PseudocodeGenerationPass() {
        this.pseudocodeService = new PseudocodeService();
    }

    @Override
    public String getName() {
        return "Pseudocode Generation";
    }

    @Override
    public void execute(AnalysisContext context) {
        log.debug("Generating pseudocode for {} functions", context.getFunctions().size());

        for (Function function : context.getFunctions().values()) {
            String pseudocode = pseudocodeService.generatePseudocode(function);
            log.trace("Generated pseudocode for {}: {} lines", function.getName(), pseudocode.split("\n").length);
        }
    }

}
