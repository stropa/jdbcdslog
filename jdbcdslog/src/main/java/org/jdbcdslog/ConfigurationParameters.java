package org.jdbcdslog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ConfigurationParameters {

    static Logger logger = LoggerFactory.getLogger(ConfigurationParameters.class);

    static long slowQueryThreshold = Long.MAX_VALUE;

    static boolean printStackTrace = true;

    static boolean logText = false;

    static Map<String, List<Class>> proxyClassesForTypes = new HashMap<String, List<Class>>();

    static {
        ClassLoader loader = ConfigurationParameters.class.getClassLoader();
        InputStream in = null;
        try {
            in = loader.getResourceAsStream("jdbcdslog.properties");
            Properties props = new Properties(System.getProperties());
            if (in != null)
                props.load(in);
            String sSlowQueryThreshold = props.getProperty("jdbcdslog.slowQueryThreshold");
            if (sSlowQueryThreshold != null && isLong(sSlowQueryThreshold))
                slowQueryThreshold = Long.parseLong(sSlowQueryThreshold);
            if (slowQueryThreshold == -1)
                slowQueryThreshold = Long.MAX_VALUE;
            String sLogText = props.getProperty("jdbcdslog.logText");
            if ("true".equalsIgnoreCase(sLogText))
                logText = true;
            String sprintStackTrace = props.getProperty("jdbcdslog.printStackTrace");
            if ("true".equalsIgnoreCase(sprintStackTrace))
                printStackTrace = true;
            readCustomProxiesConfig(props);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
        }
    }

    private static void readCustomProxiesConfig(Properties props) {
        for (String name : props.stringPropertyNames()) {
            String prefix = "jdbcdslog.proxies.for.";
            if (name.startsWith(prefix)) {
                String forTypeName = name.substring(name.lastIndexOf(".") + 1);
                String proxiesStr = props.getProperty(name);
                String[] proxyClassesNames = proxiesStr.split(",");
                List<Class> classes = new ArrayList<Class>();
                for (String proxyClassName : proxyClassesNames) {
                    try {
                        Class<?> aClass = Class.forName(proxyClassName.trim());
                        classes.add(aClass);
                    } catch (ClassNotFoundException e) {
                        logger.error("Failed to find class by name " + proxyClassName, e);
                    }
                }
                proxyClassesForTypes.put(forTypeName, classes);
            }
        }
    }

    public static void setSlowQueryThreshold(Long threshold) {
        slowQueryThreshold = threshold;
    }

    public static void setLogText(boolean alogText) {
        logText = alogText;
    }

    private static boolean isLong(String sSlowQueryThreshold) {
        try {
            Long.parseLong(sSlowQueryThreshold);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
