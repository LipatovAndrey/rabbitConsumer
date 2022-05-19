package io.airspector.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@Slf4j
@Service
public class CommandLineRunner {

    public void run(String scriptPath, Map<String, String> parameters) throws IOException, InterruptedException {
        String cmd = collectCommand(scriptPath, parameters);
        run(cmd);
    }

    public void run(String cmd) throws IOException, InterruptedException {
        log.info(cmd);
        ProcessBuilder pb = new ProcessBuilder(new String[]{"/bin/bash", "-c", cmd});
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            log.info(line);
        }
        process.waitFor();
    }

    public void writeLog(Process process) throws IOException {
        BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = buf.readLine()) != null) {
            log.info(line);
        }
    }

    public void writeErrorLog(Process process) throws IOException {
        BufferedReader errorBuffer = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String errorLine = "";
        StringBuilder builder = new StringBuilder();
        while ((errorLine = errorBuffer.readLine()) != null) {
            log.error(errorLine);
            if (errorLine.contains("Error") || errorLine.contains("error")) {
                builder.append(errorLine);
                builder.append("\\\n");
            }
        }
        if (builder.length() > 0) {
            throw new RuntimeException(builder.toString());
        }
    }

    public String collectCommand(String scriptPath, Map<String, String> parameters) {
        StringBuilder builder = new StringBuilder();
        builder.append(scriptPath);
        builder.append(" ");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey().equals("additionalParams")) {
                builder.append(" ");
                builder.append(entry.getValue());
                builder.append(" ");
            } else {
                builder.append("--");
                builder.append(entry.getKey());
                builder.append(" ");
                builder.append(entry.getValue());
                builder.append(" ");
            }
        }
        return builder.toString();
    }
}
