package com.arsen.service.project;

import com.arsen.core.event.Event;
import com.arsen.core.event.EventBus;
import com.arsen.core.event.EventType;
import com.arsen.model.project.Project;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.Instant;

@Slf4j
public class ProjectService {
    private final ObjectMapper objectMapper;
    private final EventBus eventBus;
    @Getter
    private Project currentProject;

    public ProjectService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.eventBus = EventBus.getInstance();
    }

    public void saveProject(Path path) {
        if (currentProject == null) {
            throw new IllegalStateException("No project to save");
        }

        try {
            currentProject.setModified(Instant.now());
            objectMapper.writeValue(path.toFile(), currentProject);
            eventBus.publish(Event.of(EventType.PROJECT_SAVED, path));
            log.info("Project saved to: {}", path);
        } catch (Exception e) {
            log.error("Failed to save project", e);
            throw new RuntimeException("Failed to save project", e);
        }
    }

    public Project loadProject(Path path) {
        try {
            Project project = objectMapper.readValue(path.toFile(), Project.class);
            this.currentProject = project;
            eventBus.publish(Event.of(EventType.PROJECT_LOADED, project));
            log.info("Project loaded from: {}", path);
            return project;
        } catch (Exception e) {
            log.error("Failed to load project", e);
            throw new RuntimeException("Failed to load project", e);
        }
    }

    public void createProject(String name, Path projectPath) {
        this.currentProject = Project.builder().name(name).projectPath(projectPath).created(Instant.now()).modified(Instant.now()).build();
    }

}
