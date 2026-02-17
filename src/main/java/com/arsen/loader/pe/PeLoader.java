package com.arsen.loader.pe;

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
public class PeLoader implements BinaryLoader {

    @Override
    public boolean supports(byte[] header) {
        if (header.length < 2) return false;
        return header[0] == 'M' && header[1] == 'Z';
    }

    @Override
    public BinaryFile load(Path path) throws Exception {
        log.info("Loading PE file: {}", path);
        byte[] data = Files.readAllBytes(path);
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int peOffset = buffer.getInt(0x3C);
        buffer.position(peOffset);

        int peSignature = buffer.getInt();
        if (peSignature != 0x00004550) {
            throw new IllegalArgumentException("Invalid PE signature");
        }

        short machine = buffer.getShort();
        Architecture arch = mapMachineToArchitecture(machine);

        short numberOfSections = buffer.getShort();
        buffer.position(buffer.position() + 12);
        short sizeOfOptionalHeader = buffer.getShort();
        buffer.position(buffer.position() + 2);

        long entryPoint = 0;
        int bitness = 32;

        if (sizeOfOptionalHeader > 0) {
            short magic = buffer.getShort();
            bitness = (magic == 0x20b) ? 64 : 32;
            buffer.position(buffer.position() + 14);
            entryPoint = buffer.getInt() & 0xFFFFFFFFL;
            buffer.position(buffer.position() + (bitness == 64 ? 88 : 72));
        }

        int sectionHeaderStart = peOffset + 24 + sizeOfOptionalHeader;
        List<Section> sections = parseSections(buffer, sectionHeaderStart, numberOfSections, data);

        return BinaryFile.builder().filePath(path).format(BinaryFormat.PE).architecture(arch).endianness(Endianness.LITTLE).bitness(bitness).entryPoint(Address.of(entryPoint)).sections(sections).rawData(data).build();
    }

    private List<Section> parseSections(ByteBuffer buffer, int offset, int count, byte[] data) {
        List<Section> sections = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int sectionOffset = offset + (i * 40);
            buffer.position(sectionOffset);

            byte[] nameBytes = new byte[8];
            buffer.get(nameBytes);
            String name = new String(nameBytes).trim().replace("\0", "");

            int virtualSize = buffer.getInt();
            int virtualAddress = buffer.getInt();
            int rawSize = buffer.getInt();
            int rawAddress = buffer.getInt();
            buffer.position(buffer.position() + 12);
            int characteristics = buffer.getInt();

            byte[] sectionData = new byte[Math.min(rawSize, data.length - rawAddress)];
            if (rawAddress < data.length) {
                System.arraycopy(data, rawAddress, sectionData, 0, sectionData.length);
            }

            sections.add(Section.builder().name(name).virtualAddress(Address.of(virtualAddress)).virtualSize(virtualSize).rawAddress(Address.of(rawAddress)).rawSize(rawSize).flags(characteristics).data(sectionData).build());
        }

        return sections;
    }

    private Architecture mapMachineToArchitecture(short machine) {
        return switch (machine) {
            case 0x014c -> Architecture.X86;
            case (short) 0x8664 -> Architecture.X86_64;
            case (short) 0xaa64 -> Architecture.ARM64;
            case (short) 0x01c0, (short) 0x01c2, (short) 0x01c4 -> Architecture.ARM;
            default -> Architecture.UNKNOWN;
        };
    }

    @Override
    public BinaryFormat getFormat() {
        return BinaryFormat.PE;
    }
}
