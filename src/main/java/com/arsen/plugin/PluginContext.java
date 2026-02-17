package com.arsen.plugin;

import com.arsen.core.event.EventBus;
import com.arsen.service.BinaryService;

public record PluginContext(BinaryService binaryService, EventBus eventBus) {
}
