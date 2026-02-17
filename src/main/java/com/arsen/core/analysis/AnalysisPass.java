package com.arsen.core.analysis;

public interface AnalysisPass {
    String getName();

    void execute(AnalysisContext context);
}
