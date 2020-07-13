package com.noqms.runner;

import java.util.Properties;

public class Util {
    public static Properties argsToProps(String[] args) throws Exception {
        Properties props = new Properties();
        for (String arg : args) {
            int equalsPos = arg.indexOf('=');
            if (equalsPos < 1)
                throw new Exception("Failed parsing args for key value pair: " + arg);
            String key = arg.substring(0, equalsPos);
            String value = arg.substring(equalsPos + 1);
            props.put(key, value);
        }
        return props;
    }

    public static void sleepMillis(long millis) {
        long sleptMillis = 0;
        while (sleptMillis < millis) {
            long millisStart = System.currentTimeMillis();
            try {
                Thread.sleep(millis - sleptMillis);
            } catch (Exception ex) {
            }
            sleptMillis += System.currentTimeMillis() - millisStart;
        }
    }
    
    public static long getMemoryUsedMB() {
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        return (totalMemory - freeMemory) / (1024 * 1024);
    }

    public static int getMemoryUsedPercent() {
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        return 100 - (int)(100.0F * freeMemory / totalMemory);
    }
}
