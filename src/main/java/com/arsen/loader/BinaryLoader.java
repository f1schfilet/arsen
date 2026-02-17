package com.arsen.loader;

import com.arsen.model.BinaryFormat;
import com.arsen.model.binary.BinaryFile;

import java.nio.file.Path;

public interface BinaryLoader {
    boolean supports(byte[] header);

    BinaryFile load(Path path) throws Exception;

    BinaryFormat getFormat();
}
