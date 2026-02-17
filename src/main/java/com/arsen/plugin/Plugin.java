package com.arsen.plugin;

public interface Plugin {
    String getName();

    String getVersion();

    String getDescription();

    void initialize(PluginContext context);

    void shutdown();
}
