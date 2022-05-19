package io.airspector.utils;

import io.airspector.domain.model.Facility;
import io.airspector.domain.model.FacilityModel;
import io.airspector.domain.model.Inspection;
import io.airspector.domain.model.Property;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PathUtils {

    public static String presignedUrlToKey(String url) {
        String decoded = null;
        try {
            decoded = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name());
            String withoutParams = decoded.split("\\?")[0];
            if (withoutParams.contains(".com/")) {
                return withoutParams.split(".com/")[1];
            } else {
                return withoutParams;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("can't get key by url");
    }

    public static String generateResultDirectory(Property property, Inspection inspection) {
        StringBuilder builder = new StringBuilder();
        builder.append(generateInspectionPath(inspection));
        builder.append("/");
        builder.append(property.getCode());
        return builder.toString();
    }

    public static String generateResultDirectory(Property property, Facility facility) {
        StringBuilder builder = new StringBuilder();
        builder.append(facility.getOwner().getCode());
        builder.append("/");
        builder.append(facility.getName());
        builder.append("/");
        builder.append(property.getCode());
        return builder.toString();
    }

    public static String generateResultDirectory(Property property, FacilityModel facilityModel) {
        StringBuilder builder = new StringBuilder();
        builder.append(facilityModel.getCode());
        builder.append("/");
        builder.append(property.getCode());
        return builder.toString();
    }

    public static String generateInspectionPath(Inspection inspection) {
        StringBuilder builder = new StringBuilder();
        Facility facility = inspection.getFacility();
        builder.append(facility.getOwner().getCode());
        builder.append("/");
        builder.append(facility.getName());
        builder.append("/");
        builder.append(inspection.getDateAsString());
        return builder.toString();
    }

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
}
