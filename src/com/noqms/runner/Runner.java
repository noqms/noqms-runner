package com.noqms.runner;

import java.io.FileInputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.noqms.MicroService;

public class Runner {
    public static final String PROP_MICRO_CONFIG_PATH = "noqms.microConfigPath";

    private static final int FILE_CHECK_INTERVAL_MILLIS = 60000;
    private static final Logger logger = new Logger("Runner");

    public static void main(String[] args) {
        try {
            new Runner(Util.argsToProps(args)).run();
        } catch (Exception ex) {
            logger.error("Startup exception", ex);
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
            throw new Exception("Argument noqms.microConfigPath is required");
        this.microConfigPath = Path.of(configPath);
    }

    public void run() throws Exception {
        logger.info("Started: " + props);

        long lastFileCheckTimeMillis = 0;
        while (!exiting.get()) {
            if (System.currentTimeMillis() - lastFileCheckTimeMillis > FILE_CHECK_INTERVAL_MILLIS) {
                Files.list(microConfigPath).filter(path -> path.toString().endsWith(".micro")).forEach(path -> {
                    new Thread(() -> processFile(path)).start();
                });
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
            logger.info("Reading " + fileString);
            MicroService oldMicro = microsByFilePath.remove(fileString);
            if (oldMicro != null) {
                try {
                    oldMicro.drain();
                } catch (Throwable th) {
                    logger.error("Drain exception", th);
                }
                try {
                    oldMicro.destroy();
                } catch (Throwable th) {
                    logger.error("Destroy exception", th);
                }
            }
            try {
                MicroService micro = loadMicro(microFile); // takes noqms.emitterIntervalSeconds (default 2 seconds) to complete
                fileTimesByFilePath.put(fileString, lastModified);
                microsByFilePath.put(fileString, micro);
            } catch (Exception ex) {
                logger.error("Failed loading the microservice in " + fileString, null);
            }
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
                logger.error("Uncaught exception", th);
            } catch (Throwable th2) {
            }
            exiting.set(true);
        }
    }

    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            logger.info("Stopping");
            AtomicInteger threadCount = new AtomicInteger(microsByFilePath.size());
            microsByFilePath.values().forEach(microService -> new Thread(() -> {
                try {
                    microService.drain();
                } catch (Throwable th) {
                    logger.error("Drain exception", th);
                }
                try {
                    microService.destroy();
                } catch (Throwable th) {
                    logger.error("Destroy exception", th);
                }
                threadCount.decrementAndGet();
            }).start());
            while (threadCount.get() > 0)
                Util.sleepMillis(1000);
            logger.info("Stopped");
        }
    }
}
