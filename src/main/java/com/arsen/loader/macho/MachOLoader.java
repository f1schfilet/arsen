package com.arsen.loader.macho;

import com.arsen.loader.BinaryLoader;
import com.arsen.model.*;
import com.arsen.model.binary.BinaryFile;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MachOLoader implements BinaryLoader {

    private static final int MH_MAGIC = 0xfeedface;
    private static final int MH_MAGIC_64 = 0xfeedfacf;
    private static final int MH_CIGAM = 0xcefaedfe;
    private static final int MH_CIGAM_64 = 0xcffaedfe;

    @Override
    public boolean supports(byte[] header) {
        if (header.length < 4) return false;
        ByteBuffer buffer = ByteBuffer.wrap(header);
        int magic = buffer.getInt();
        return magic == MH_MAGIC || magic == MH_MAGIC_64 || magic == MH_CIGAM || magic == MH_CIGAM_64;
    }

    @Override
    public BinaryFile load(Path path) throws Exception {
        log.info("Loading Mach-O file: {}", path);
        byte[] data = Files.readAllBytes(path);
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int magic = buffer.getInt();
        boolean is64Bit = (magic == MH_MAGIC_64 || magic == MH_CIGAM_64);
        boolean needsSwap = (magic == MH_CIGAM || magic == MH_CIGAM_64);

        if (needsSwap) {
            buffer.order(ByteOrder.BIG_ENDIAN);
        } else {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        int cputype = buffer.getInt();
        int cpusubtype = buffer.getInt();
        int filetype = buffer.getInt();
        int ncmds = buffer.getInt();
        int sizeofcmds = buffer.getInt();
        int flags = buffer.getInt();

        if (is64Bit) {
            buffer.getInt();
        }

        Architecture arch = mapCpuTypeToArchitecture(cputype);

        List<Section> sections = new ArrayList<>();
        long entryPoint = 0;

        return BinaryFile.builder().filePath(path).format(BinaryFormat.MACH_O).architecture(arch).endianness(needsSwap ? Endianness.BIG : Endianness.LITTLE).bitness(is64Bit ? 64 : 32).entryPoint(Address.of(entryPoint)).sections(sections).rawData(data).build();
    }

    private Architecture mapCpuTypeToArchitecture(int cputype) {
        return switch (cputype) {
            case 7 -> Architecture.X86;
            case 0x01000007 -> Architecture.X86_64;
            case 12 -> Architecture.ARM;
            case 0x0100000C -> Architecture.ARM64;
            case 18 -> Architecture.POWERPC;
            default -> Architecture.UNKNOWN;
        };
    }

    @Override
    public BinaryFormat getFormat() {
        return BinaryFormat.MACH_O;
    }
}
