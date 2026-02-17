package com.arsen.service.analysis;

import com.arsen.core.event.Event;
import com.arsen.core.event.EventBus;
import com.arsen.core.event.EventType;
import com.arsen.model.Address;
import com.arsen.model.Symbol;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SymbolService {
    private final Map<Address, Symbol> symbols;
    private final EventBus eventBus;

    public SymbolService() {
        this.symbols = new ConcurrentHashMap<>();
        this.eventBus = EventBus.getInstance();
    }

    public void addSymbol(Symbol symbol) {
        symbols.put(symbol.getAddress(), symbol);
        eventBus.publish(Event.of(EventType.SYMBOL_ADDED, symbol));
        log.debug("Added symbol: {} at {}", symbol.getName(), symbol.getAddress());
    }

    public void renameSymbol(Address address, String newName) {
        Symbol symbol = symbols.get(address);
        if (symbol != null) {
            symbol.setName(newName);
            eventBus.publish(Event.of(EventType.SYMBOL_RENAMED, symbol));
            log.info("Renamed symbol at {} to {}", address, newName);
        }
    }

    public Symbol getSymbol(Address address) {
        return symbols.get(address);
    }

    public Map<Address, Symbol> getAllSymbols() {
        return symbols;
    }
}
