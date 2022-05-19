package io.airspector.consumer.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;


@Service
public class FileService {

    public void upload(byte[] byteArray, String filePath) {
        File file = new File(filePath);
        String theDirStr = file.getAbsoluteFile().getParent();
        File dir = new File(theDirStr);
        if (!dir.exists()) {
            System.out.println("create dir " + theDirStr);
            dir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            System.out.println("upload file " + filePath);
            fos.write(byteArray);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Boolean needToUpload(String filePath) {
        File file = new File(filePath);
        String format = PathUtils.getFormat(filePath);
        if ((file.exists() && format.equals("html") || !file.exists())) {
            System.out.println("file not exist and html or not exist " + filePath);
            return true;
        } else {
            System.out.println("not need to upload " + filePath);
            return false;
        }
    }
}
