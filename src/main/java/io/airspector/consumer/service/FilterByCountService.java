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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class S3Service {
    public static final String BUCKET_NAME = "airspector";

    @Autowired
    private AmazonS3 s3client;

    public void upload(byte[] byteArray, String filePath) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(BUCKET_NAME, filePath);
        InitiateMultipartUploadResult result = s3client.initiateMultipartUpload(request);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
        s3client.putObject(result.getBucketName(), result.getKey(), inputStream, new ObjectMetadata());

    }




    public List<String> getKeys(String directory) {
        ListObjectsV2Result objects = s3client.listObjectsV2(BUCKET_NAME, directory);
        return objects.getObjectSummaries()
                .stream()
                .map(s3ObjectSummary -> s3ObjectSummary.getKey())
                .collect(Collectors.toList());
    }

    public String generatePresignedUrl(String objectKey) {
        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = Instant.now().toEpochMilli();
        // Set the presigned URL to expire after one hour.
        expTimeMillis += 1000 * 60 * 60;
        expiration.setTime(expTimeMillis);
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(BUCKET_NAME, objectKey)
                        .withMethod(com.amazonaws.HttpMethod.GET)
                        .withExpiration(expiration);
        URL url = s3client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    public byte[] read(String filePath) {
        S3Object object = s3client.getObject(new GetObjectRequest(BUCKET_NAME, filePath));
        try {
            return IOUtils.toByteArray(object.getObjectContent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("can't read file from s3");
    }

    public boolean isPitchLessThirty(String filePath) {
        S3Object object = s3client.getObject(new GetObjectRequest(BUCKET_NAME, filePath));
        boolean isPitchLessThirty = false;
        try {
            ExifTool exifTool = new ExifToolBuilder().build();
            File targetFile = File.createTempFile("temp", ".JPG");
            byte[] content = IOUtils.toByteArray(object.getObjectContent());
            OutputStream outStream = new FileOutputStream(targetFile);
            outStream.write(content);
            Map<com.thebuzzmedia.exiftool.Tag, String> meta = exifTool.getImageMeta(targetFile);
            for (Map.Entry<com.thebuzzmedia.exiftool.Tag, String> entry : meta.entrySet()) {
                com.thebuzzmedia.exiftool.Tag tag = entry.getKey();
                String s = entry.getValue();
                System.out.println(tag);
                if (tag.getName().equals("CameraOrientationFLUPitch")) {
                    Double pitch = Double.valueOf(s);
                    if (pitch < 30) {
                        isPitchLessThirty = true;
                    }
                }
            }
            targetFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isPitchLessThirty;
    }

}
