package io.airspector.consumer.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.ExifToolBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FilterByCountService {
    public static final Integer PAGE_LIMIT = 50;

    @Autowired
    private S3Service s3Service;

    public void filterByCount(String inputDir, String outputDir) {
        List<String> keys = s3Service.getKeys(inputDir);
        Collections.shuffle(keys);
        List<String> limitedKeys =  keys.stream().limit(PAGE_LIMIT).collect(Collectors.toList());
        limitedKeys.forEach(s -> {
            byte[] copy = s3Service.read(s);
            s3Service.upload(copy, outputDir+ "/"+ s.replace(inputDir + "/", "") );
        });

    }
}
