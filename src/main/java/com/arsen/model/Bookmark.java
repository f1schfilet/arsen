package com.arsen.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Bookmark {
    Address address;
    String description;
}
