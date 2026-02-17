package com.arsen.plugin;

import com.arsen.core.pseudocode.PseudocodeService;
import com.arsen.model.disassembly.Function;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PseudocodePlugin implements Plugin {
    private PseudocodeService pseudocodeService;
    private PluginContext context;

    @Override
    public String getName() {
        return "Pseudocode Generator";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Generates human-readable pseudocode from disassembled functions";
    }

    @Override
    public void initialize(PluginContext context) {
        this.context = context;
        this.pseudocodeService = new PseudocodeService();
        log.info("Pseudocode plugin initialized");
    }

    @Override
    public void shutdown() {
        if (pseudocodeService != null) {
            pseudocodeService.clearCache();
        }
        log.info("Pseudocode plugin shutdown");
    }

    public String generatePseudocode(Function function) {
        if (pseudocodeService == null) {
            throw new IllegalStateException("Plugin not initialized");
        }
        return pseudocodeService.generatePseudocode(function);
    }

    public Map<Function, String> generateAllPseudocode() {
        if (context == null || context.binaryService().getCurrentAnalysis() == null) {
            throw new IllegalStateException("No analysis available");
        }

        var functions = context.binaryService().getCurrentAnalysis().getFunctions().values();
        return pseudocodeService.generateAll(functions);
    }
}
