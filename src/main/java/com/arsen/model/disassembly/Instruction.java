package com.arsen.model.disassembly;

import com.arsen.model.Address;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class Instruction {
    Address address;
    byte[] bytes;
    String mnemonic;

    @Singular
    List<Operand> operands;

    int size;
    InstructionType type;
    Address targetAddress;

    public String getFullText() {
        if (operands.isEmpty()) {
            return mnemonic;
        }
        StringBuilder sb = new StringBuilder(mnemonic);
        sb.append(" ");
        for (int i = 0; i < operands.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(operands.get(i).getText());
        }
        return sb.toString();
    }

    public String getBytesAsHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
