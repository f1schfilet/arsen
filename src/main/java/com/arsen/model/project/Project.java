package com.arsen.model.project;

import com.arsen.model.Address;
import com.arsen.model.Bookmark;
import com.arsen.model.Comment;
import com.arsen.model.Symbol;
import com.arsen.model.binary.BinaryFile;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class Project {
    String name;
    Path projectPath;
    BinaryFile binaryFile;
    Instant created;
    Instant modified;

    @Singular
    Map<Address, Symbol> symbols;

    @Singular
    Map<Address, Comment> comments;

    @Singular
    List<Bookmark> bookmarks;
}
