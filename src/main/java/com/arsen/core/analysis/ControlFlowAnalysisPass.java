package com.arsen.core.analysis;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ControlFlowAnalysisPass implements AnalysisPass {

    @Override
    public String getName() {
        return "Control Flow Analysis";
    }

    @Override
    public void execute(AnalysisContext context) {
        log.debug("Analyzing control flow for {} functions", context.getFunctions().size());
    }
}
