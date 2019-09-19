package com.optimizely.ab.android.test_app.bdd.support;

import org.yaml.snakeyaml.Yaml;

public class Utils {
    public static Object parseYAML(String args) {
        if ("NULL".equals(args) || args.isEmpty()) {
            return null;
        }
        Yaml yaml = new Yaml();
        return yaml.load(args);
    }
}
