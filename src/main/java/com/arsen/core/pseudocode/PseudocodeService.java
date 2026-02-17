package com.arsen.core.pseudocode;

import com.arsen.model.disassembly.Function;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PseudocodeService {
    private final PseudocodeGenerator generator;
    private final SyntaxHighlighter highlighter;
    private final Map<Function, String> cache;

    public PseudocodeService() {
        this.generator = new PseudocodeGenerator();
        this.highlighter = new SyntaxHighlighter();
        this.cache = new ConcurrentHashMap<>();
    }

    public String generatePseudocode(Function function) {
        if (function == null) {
            return "";
        }

        return cache.computeIfAbsent(function, func -> {
            log.debug("Generating pseudocode for function: {}", func.getName());

            String rawPseudocode = generator.generatePseudocode(func);

            if (rawPseudocode == null || rawPseudocode.trim().isEmpty()) {
                log.warn("Generator produced empty pseudocode for function: {}", func.getName());
                return "";
            }

            String highlighted = highlighter.highlight(rawPseudocode);

            if (highlighted == null || highlighted.trim().isEmpty()) {
                log.warn("Highlighter cleared pseudocode for function: {}, returning raw", func.getName());
                return rawPseudocode;
            }

            return highlighted;
        });
    }

    public void clearCache() {
        cache.clear();
    }

    public Map<Function, String> generateAll(Iterable<Function> functions) {
        Map<Function, String> results = new ConcurrentHashMap<>();
        for (Function function : functions) {
            results.put(function, generatePseudocode(function));
        }
        return results;
    }
}
