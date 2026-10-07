package com.mx.kylgis.kitchenscr.config.provisioning;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves !secret.key references without modifying the raw properties. */
public final class SecretResolver {
    private static final Pattern REF = Pattern.compile("!secret\\.([A-Za-z0-9_.-]+)");

    private SecretResolver() { }

    public static Properties resolve(Properties raw, Properties secrets) throws IOException {
        Properties out = new Properties();
        out.putAll(raw);
        for (String key : raw.stringPropertyNames()) {
            String value = raw.getProperty(key);
            if (value == null || value.indexOf("!secret.") < 0) {
                continue;
            }
            Matcher m = REF.matcher(value);
            StringBuffer b = new StringBuffer();
            while (m.find()) {
                String secretKey = m.group(1);
                String secret = secrets == null ? null : secrets.getProperty(secretKey);
                if (secret == null) {
                    throw new IOException("Missing KylGis secret: " + secretKey + " required by " + key);
                }
                m.appendReplacement(b, Matcher.quoteReplacement(secret));
            }
            m.appendTail(b);
            out.setProperty(key, b.toString());
        }
        return out;
    }

    public static boolean containsReference(String value) {
        return value != null && REF.matcher(value).find();
    }
}
