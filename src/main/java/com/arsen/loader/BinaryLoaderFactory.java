package com.arsen.loader;

import com.arsen.loader.elf.ElfLoader;
import com.arsen.loader.macho.MachOLoader;
import com.arsen.loader.pe.PeLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BinaryLoaderFactory {
    private static final List<BinaryLoader> loaders = new ArrayList<>();

    static {
        loaders.add(new PeLoader());
        loaders.add(new ElfLoader());
        loaders.add(new MachOLoader());
    }

    public static BinaryLoader getLoader(Path path) throws IOException {
        byte[] header = readHeader(path);

        for (BinaryLoader loader : loaders) {
            if (loader.supports(header)) {
                log.info("Selected loader: {} for file: {}", loader.getFormat(), path);
                return loader;
            }
        }

        throw new UnsupportedOperationException("No loader found for binary format");
    }

    private static byte[] readHeader(Path path) throws IOException {
        byte[] buffer = new byte[512];
        try (var stream = Files.newInputStream(path)) {
            int read = stream.read(buffer);
            if (read < buffer.length) {
                byte[] result = new byte[read];
                System.arraycopy(buffer, 0, result, 0, read);
                return result;
            }
        }
        return buffer;
    }
}
