package com.promoitotp.util;

import java.io.InputStream;
import java.util.Properties;

public final class Props {

    private Props() {}

    public static Properties load(String filename) {
        try (InputStream in = Props.class.getClassLoader().getResourceAsStream(filename)) {
            if (in == null) throw new IllegalStateException("Classpath resource not found: " + filename);
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read " + filename, e);
        }
    }

    public static String get(Properties p, String key) {
        String env = System.getenv(key.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) return env;
        String val = p.getProperty(key);
        if (val == null) throw new IllegalStateException("Missing property: " + key);
        return val;
    }

    public static String get(Properties p, String key, String defaultVal) {
        String env = System.getenv(key.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) return env;
        return p.getProperty(key, defaultVal);
    }

    public static int getInt(Properties p, String key, int defaultVal) {
        return Integer.parseInt(get(p, key, String.valueOf(defaultVal)));
    }
}
