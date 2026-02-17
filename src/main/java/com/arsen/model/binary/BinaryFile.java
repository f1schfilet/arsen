package com.arsen.model.binary;

import com.arsen.model.*;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Data
@Builder
public class BinaryFile {
    Path filePath;
    BinaryFormat format;
    Architecture architecture;
    Endianness endianness;
    int bitness;
    Address entryPoint;

    @Singular
    List<Section> sections;

    @Singular
    List<Import> imports;

    @Singular
    List<Export> exports;

    byte[] rawData;

    public Optional<Section> getSectionByAddress(Address address) {
        return sections.stream().filter(s -> s.containsAddress(address)).findFirst();
    }

    public Optional<Section> getSectionByName(String name) {
        return sections.stream().filter(s -> s.getName().equals(name)).findFirst();
    }
}
