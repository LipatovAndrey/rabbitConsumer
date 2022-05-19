package io.airspector.consumer.service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PathUtils {


    public static List<String> filterByFormat(List<String> keys, String format) {
        if (format != null) {
            return filterByFormat(
                    keys,
                    Arrays.asList(format
                            .replace(" ", "")
                            .split(",")));
        } else {
            return keys;
        }
    }

    public static List<String> filterByFormat(List<String> keys, List<String> format) {
        return keys.stream()
                .filter(s -> {
                    if (s.split("\\.").length > 1) {
                        Boolean constains = format.contains(s.split("\\.")[1]);
                        return constains;
                    } else {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    public static List<String> filterByIsSubDirectory(List<String> keys, String directory) {
        return keys.parallelStream()
                .filter(s -> !isSubDirectory(s, directory))
                .collect(Collectors.toList());
    }

    public static Boolean isSubDirectory(String key, String targetDirectory) {
        return key.replace(targetDirectory + "/", "").contains("/");
    }

    public static String getFileName(String key, String directory) {
        return key.replace(directory + "/", "");
    }

    public static String getFormat(String key) {
        return key.split("\\.")[1];
    }
}
