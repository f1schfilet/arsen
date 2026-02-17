package com.arsen.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogManager {

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
}
