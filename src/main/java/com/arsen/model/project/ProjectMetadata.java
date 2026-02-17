package com.arsen.model.project;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ProjectMetadata {
    String projectName;
    String binaryPath;
    Instant created;
    Instant lastModified;
    String version;
}
