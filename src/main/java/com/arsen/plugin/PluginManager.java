package com.arsen.plugin;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PluginManager {
    private final List<Plugin> plugins;
    private final PluginContext context;

    public PluginManager(PluginContext context) {
        this.plugins = new ArrayList<>();
        this.context = context;
    }

    public void loadPlugin(Plugin plugin) {
        try {
            plugin.initialize(context);
            plugins.add(plugin);
            log.info("Loaded plugin: {} v{}", plugin.getName(), plugin.getVersion());
        } catch (Exception e) {
            log.error("Failed to load plugin: {}", plugin.getName(), e);
        }
    }

    public void unloadAll() {
        for (Plugin plugin : plugins) {
            try {
                plugin.shutdown();
            } catch (Exception e) {
                log.error("Error shutting down plugin: {}", plugin.getName(), e);
            }
        }
        plugins.clear();
    }

    public List<Plugin> getPlugins() {
        return new ArrayList<>(plugins);
    }
}
