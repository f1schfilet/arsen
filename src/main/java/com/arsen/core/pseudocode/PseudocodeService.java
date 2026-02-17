package com.arsen.core.pseudocode;

import com.arsen.model.disassembly.Function;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PseudocodeService {
    private final PseudocodeGenerator generator;
    private final Map<Function, String> cache;

    public PseudocodeService() {
        this.generator = new PseudocodeGenerator();
        this.cache = new ConcurrentHashMap<>();
    }

    public String generatePseudocode(Function function) {
        if (function == null) {
            return "";
        }

        return cache.computeIfAbsent(function, func -> {
            log.debug("Generating pseudocode for function: {}", func.getName());
            return generator.generatePseudocode(func);
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
