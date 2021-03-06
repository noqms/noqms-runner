/*
 * Copyright 2020 Stanley Barzee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.noqms.runner;

import java.util.ArrayDeque;

import com.noqms.LogListener;

public class Logger extends Thread implements LogListener {
    private final ArrayDeque<LogEntry> logEntries = new ArrayDeque<>();
    private final org.slf4j.Logger logger;

    public Logger(String name) {
        logger = org.slf4j.LoggerFactory.getLogger(name);
        setDaemon(true);
        start();
    }

    public void debug(String text) {
        addEntry(Level.DEBUG, text, null);
    }

    public void info(String text) {
        addEntry(Level.INFO, text, null);
    }

    public void warn(String text) {
        addEntry(Level.WARN, text, null);
    }

    public void error(String text, Throwable cause) {
        addEntry(Level.ERROR, text, cause);
    }

    private void addEntry(Level level, String text, Throwable cause) {
        synchronized (logEntries) {
            logEntries.addLast(new LogEntry(level, text, cause));
            logEntries.notify();
        }
    }

    public void run() {
        while (true) {
            LogEntry logEntry = null;
            synchronized (logEntries) {
                logEntry = logEntries.pollFirst();
                if (logEntry == null) {
                    try {
                        logEntries.wait();
                    } catch (Exception ex) {
                    }
                }
            }
            if (logEntry != null) {
                Throwable cause = logEntry.cause;
                String causeMessage = cause == null ? "" : (": " + cause.toString());
                String text = logEntry.text + causeMessage;
                switch (logEntry.level) {
                case DEBUG:
                    logger.debug(text);
                    break;
                case INFO:
                    logger.info(text);
                    break;
                case WARN:
                    logger.warn(text);
                    break;
                case ERROR:
                    logger.error(text, cause);
                    break;
                }
            }
        }
    }

    private enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    private static class LogEntry {
        private final Level level;
        private final String text;
        private final Throwable cause;

        public LogEntry(Level level, String text, Throwable cause) {
            this.level = level;
            this.text = text;
            this.cause = cause;
        }
    }
}
