package com.noqms.runner;

import java.io.FileInputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.noqms.MicroService;

public class Runner {
    public static final String PROP_MICRO_CONFIG_PATH = "noqms.microConfigPath";

    private static final int FILE_CHECK_INTERVAL_MILLIS = 10000;
    private static final Logger logger = new Logger("Runner");

    public static void main(String[] args) {
        try {
            new Runner(Util.argsToProps(args)).run();
        } catch (Exception ex) {
            logger.logError("Startup exception", ex);
            Util.sleepMillis(100);
            System.exit(-1);
        }
    }

    private final AtomicBoolean exiting = new AtomicBoolean();
    private final Map<String, MicroService> microsByFilePath = new ConcurrentHashMap<>();
    private final Map<String, Long> fileTimesByFilePath = new ConcurrentHashMap<>();
    private final Properties props;
    private final Path microConfigPath;

    public Runner(Properties props) throws Exception {
        this.props = props;
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHook());
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        String configPath = props.getProperty(PROP_MICRO_CONFIG_PATH);
        if (configPath == null)
            throw new Exception("Argument microConfigPath is required");
        this.microConfigPath = Path.of(configPath);
    }

    public void run() throws Exception {
        logger.logInfo("Started: " + props);

        long lastFileCheckTimeMillis = 0;
        while (!exiting.get()) {
            if (System.currentTimeMillis() - lastFileCheckTimeMillis > FILE_CHECK_INTERVAL_MILLIS) {
                Files.list(microConfigPath).filter(path -> path.toString().endsWith(".micro")).forEach(path -> {
                    processFile(path);
                });
                logger.logInfo("microsLoaded=" + microsByFilePath.size());
                lastFileCheckTimeMillis = System.currentTimeMillis();
            }
            Util.sleepMillis(1000);
        }
    }

    private void processFile(Path microFile) {
        if (exiting.get())
            return;
        long lastModified = microFile.toFile().lastModified();
        String fileString = microFile.toString();
        Long fileTime = fileTimesByFilePath.get(fileString);
        if (fileTime == null || lastModified > fileTime) {
            logger.logInfo("Reading " + fileString);
            MicroService oldMicro = microsByFilePath.remove(fileString);
            if (oldMicro != null) {
                oldMicro.drain(); // takes noqms.serviceUnavailableSeconds (default 5 seconds) to complete
                oldMicro.stop();
            }
            MicroService micro = null;
            try {
                micro = loadMicro(microFile); // takes noqms.emitterIntervalSeconds (default 2 seconds) to complete
                fileTimesByFilePath.put(fileString, lastModified);
            } catch (Exception ex) {
                logger.logError("Failed loading the microservice in " + fileString, null);
            }
            if (micro != null)
                microsByFilePath.put(fileString, micro);
        }
    }

    private MicroService loadMicro(Path microFile) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream(microFile.toString()));
        if ("false".equals(props.getProperty("enable")))
            return null;
        return com.noqms.Starter.start(props, logger);
    }

    private class UncaughtExceptionHook implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable th) {
            try {
                System.err.println("Uncaught exception: " + th.getMessage());
                th.printStackTrace();
                logger.logError("Uncaught exception", th);
            } catch (Throwable th2) {
            }
            exiting.set(true);
        }
    }

    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            logger.logInfo("Stopping");
            microsByFilePath.values().forEach(MicroService::drain);
            microsByFilePath.values().forEach(MicroService::stop);
            logger.logInfo("Stopped");
            Util.sleepMillis(100);
        }
    }
}
