package com.arsen.core.analysis;

import com.arsen.model.Section;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StringAnalysisPass implements AnalysisPass {

    private static final int MIN_STRING_LENGTH = 4;

    @Override
    public String getName() {
        return "String Analysis";
    }

    @Override
    public void execute(AnalysisContext context) {
        for (Section section : context.getBinaryFile().getSections()) {
            if (section.isReadable() && section.getData() != null) {
                extractStrings(section.getData(), context);
            }
        }
        log.debug("Found {} strings", context.getStrings().size());
    }

    private void extractStrings(byte[] data, AnalysisContext context) {
        List<Byte> currentString = new ArrayList<>();

        for (byte b : data) {
            if (b >= 32 && b <= 126) {
                currentString.add(b);
            } else {
                if (currentString.size() >= MIN_STRING_LENGTH) {
                    byte[] strBytes = new byte[currentString.size()];
                    for (int i = 0; i < currentString.size(); i++) {
                        strBytes[i] = currentString.get(i);
                    }
                    context.addString(new String(strBytes, StandardCharsets.US_ASCII));
                }
                currentString.clear();
            }
        }
    }
}
