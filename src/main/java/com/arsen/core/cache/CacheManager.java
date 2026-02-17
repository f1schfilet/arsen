package com.arsen.core.cache;

import com.arsen.model.Address;
import com.arsen.model.disassembly.Instruction;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public class CacheManager {
    private static final CacheManager INSTANCE = new CacheManager();

    private final Cache<Address, Instruction> instructionCache;
    private final Cache<String, Object> analysisCache;

    private CacheManager() {
        instructionCache = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(30, TimeUnit.MINUTES).build();

        analysisCache = CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(30, TimeUnit.MINUTES).build();
    }

    public static CacheManager getInstance() {
        return INSTANCE;
    }

    public Instruction getInstruction(Address address) {
        return instructionCache.getIfPresent(address);
    }

    public void putInstruction(Address address, Instruction instruction) {
        instructionCache.put(address, instruction);
    }

    public Object getAnalysisResult(String key) {
        return analysisCache.getIfPresent(key);
    }

    public void putAnalysisResult(String key, Object result) {
        analysisCache.put(key, result);
    }

    public void clearAll() {
        instructionCache.invalidateAll();
        analysisCache.invalidateAll();
    }
}
