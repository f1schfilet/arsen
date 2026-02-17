package com.arsen.loader.elf;

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
public class ElfLoader implements BinaryLoader {

    @Override
    public boolean supports(byte[] header) {
        if (header.length < 4) return false;
        return header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F';
    }

    @Override
    public BinaryFile load(Path path) throws Exception {
        log.info("Loading ELF file: {}", path);
        byte[] data = Files.readAllBytes(path);
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int elfClass = data[4];
        int elfData = data[5];

        ByteOrder byteOrder = (elfData == 1) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        buffer.order(byteOrder);

        boolean is64Bit = (elfClass == 2);
        int bitness = is64Bit ? 64 : 32;

        buffer.position(16);
        short type = buffer.getShort();
        short machine = buffer.getShort();
        buffer.getInt();

        long entryPoint;
        long phoff;
        long shoff;

        if (is64Bit) {
            entryPoint = buffer.getLong();
            phoff = buffer.getLong();
            shoff = buffer.getLong();
        } else {
            entryPoint = buffer.getInt() & 0xFFFFFFFFL;
            phoff = buffer.getInt() & 0xFFFFFFFFL;
            shoff = buffer.getInt() & 0xFFFFFFFFL;
        }

        buffer.getInt();
        short ehsize = buffer.getShort();
        short phentsize = buffer.getShort();
        short phnum = buffer.getShort();
        short shentsize = buffer.getShort();
        short shnum = buffer.getShort();
        short shstrndx = buffer.getShort();

        List<Section> sections = parseSections(buffer, shoff, shnum, shentsize, shstrndx, is64Bit, data);

        return BinaryFile.builder().filePath(path).format(BinaryFormat.ELF).architecture(mapMachineToArchitecture(machine)).endianness(elfData == 1 ? Endianness.LITTLE : Endianness.BIG).bitness(bitness).entryPoint(Address.of(entryPoint)).sections(sections).rawData(data).build();
    }

    private List<Section> parseSections(ByteBuffer buffer, long shoff, int shnum, int shentsize, int shstrndx, boolean is64Bit, byte[] data) {
        List<Section> sections = new ArrayList<>();

        byte[] stringTableData = null;
        if (shstrndx < shnum) {
            buffer.position((int) (shoff + shstrndx * shentsize));
            buffer.getInt();
            buffer.getInt();

            long strtabOffset;
            long strtabSize;

            if (is64Bit) {
                buffer.getLong();
                strtabOffset = buffer.getLong();
                strtabSize = buffer.getLong();
            } else {
                buffer.getInt();
                strtabOffset = buffer.getInt() & 0xFFFFFFFFL;
                strtabSize = buffer.getInt() & 0xFFFFFFFFL;
            }

            stringTableData = new byte[(int) strtabSize];
            System.arraycopy(data, (int) strtabOffset, stringTableData, 0, (int) strtabSize);
        }

        for (int i = 0; i < shnum; i++) {
            buffer.position((int) (shoff + i * shentsize));

            int nameOffset = buffer.getInt();
            int type = buffer.getInt();

            long flags;
            long addr;
            long offset;
            long size;

            if (is64Bit) {
                flags = buffer.getLong();
                addr = buffer.getLong();
                offset = buffer.getLong();
                size = buffer.getLong();
            } else {
                flags = buffer.getInt() & 0xFFFFFFFFL;
                addr = buffer.getInt() & 0xFFFFFFFFL;
                offset = buffer.getInt() & 0xFFFFFFFFL;
                size = buffer.getInt() & 0xFFFFFFFFL;
            }

            String name = "SECTION_" + i;
            if (stringTableData != null && nameOffset < stringTableData.length) {
                int endIdx = nameOffset;
                while (endIdx < stringTableData.length && stringTableData[endIdx] != 0) {
                    endIdx++;
                }
                name = new String(stringTableData, nameOffset, endIdx - nameOffset);
            }

            byte[] sectionData = new byte[(int) Math.min(size, data.length - offset)];
            if (offset < data.length) {
                System.arraycopy(data, (int) offset, sectionData, 0, sectionData.length);
            }

            sections.add(Section.builder().name(name).virtualAddress(Address.of(addr)).virtualSize(size).rawAddress(Address.of(offset)).rawSize(size).flags((int) flags).data(sectionData).build());
        }

        return sections;
    }

    private Architecture mapMachineToArchitecture(short machine) {
        return switch (machine) {
            case 3 -> Architecture.X86;
            case 62 -> Architecture.X86_64;
            case 40 -> Architecture.ARM;
            case (short) 183 -> Architecture.ARM64;
            case 8 -> Architecture.MIPS;
            case 20, 21 -> Architecture.POWERPC;
            default -> Architecture.UNKNOWN;
        };
    }

    @Override
    public BinaryFormat getFormat() {
        return BinaryFormat.ELF;
    }
}
