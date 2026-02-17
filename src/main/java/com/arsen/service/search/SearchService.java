package com.arsen.service.search;

import com.arsen.model.Address;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SearchService {

    public List<SearchResult> searchBytes(byte[] data, byte[] pattern) {
        List<SearchResult> results = new ArrayList<>();

        for (int i = 0; i <= data.length - pattern.length; i++) {
            if (matchesPattern(data, i, pattern)) {
                results.add(new SearchResult(Address.of(i), pattern.length));
            }
        }

        log.debug("Found {} matches for byte pattern", results.size());
        return results;
    }

    public List<SearchResult> searchString(byte[] data, String text) {
        byte[] pattern = text.getBytes();
        return searchBytes(data, pattern);
    }

    private boolean matchesPattern(byte[] data, int offset, byte[] pattern) {
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    public record SearchResult(Address address, int length) {
    }
}
