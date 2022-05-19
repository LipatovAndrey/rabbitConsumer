package io.airspector.consumer.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.airspector.consumer.dto.ResultDto;
import io.airspector.consumer.service.CommandLineRunner;
import io.airspector.consumer.service.FileService;
import io.airspector.consumer.service.PathUtils;
import io.airspector.consumer.service.S3Service;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RabbitConsumer {

    public static final String EXCHANGE = "";
    public static final String DETECT_ANTENNAS_QUEUE = "airspector.antennas_input";
    public static final String LED_QUEUE = "airspector.led_input";
    public static final String FLANGES_QUEUE = "airspector.flanges_input";
    public static final String MOVE_REPORT_QUEUE = "airspector.move_report_input";
    public static final String CORROSION_QUEUE = "airspector.corrosion_input";
    public static final String COMPRESSOR_QUEUE = "airspector.compression_input";
    public static final String FILTER_BY_PITCH_QUEUE = "airspector.filter_by_pitch_input";
    public static final String FILTER_BY_HEIGHT_QUEUE = "airspector.filter_by_height_input";
    public static final String FILTER_BY_COUNT_QUEUE = "airspector.filter_by_count_input";
    public static final String GENERATE_PARAMS_QUEUE = "airspector.generate_params_input";

    public static final String ANTENNAS_SRC = "cd /home/ubuntu/src/antennas && source activate pytorch_latest_p37 &&sudo python3 main.py";
    public static final String LED_SRC = "cd /home/ubuntu/src/led && source activate pytorch_latest_p37 &&sudo python3 main.py";
    public static final String FLANGES_SRC = "cd /home/ubuntu/src/flanges && source activate pytorch_latest_p37 &&sudo python3 main.py";
    public static final String CORROSION_SRC = "cd /home/ubuntu/src/corrosion2 && source activate pytorch_latest_p37 &&sudo python3 inference.py";
    public static final String COMPRESSOR_SRC = "cd /home/ubuntu/src/compressor &&sudo python3 main.py";
    public static final String MOVE_SRC = "cd /home/ubuntu/src/move &&sudo python3 main.py";


    public static final Integer PAGE_LIMIT = 30;

    @Autowired
    private CommandLineRunner commandLineRunner;

    @Autowired
    private RabbitProducer rabbitProducer;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private FileService fileService;


    @RabbitListener(queues = FILTER_BY_PITCH_QUEUE, concurrency = "1")
    public void receiveFilterByPitch(Message message) {
        String correlationKey = getCorrelationId(message);
        String replayTo = message.getMessageProperties().getReplyTo();
        Map<String, String> result = new HashMap<>();
        try {
            Map<String, String> parameters = readValue(message);
            String inputDir = parameters.get("inputDir");
            List<String> keys = s3Service.getKeys(inputDir);
            keys.stream().forEach(s -> {
                if (s3Service.isPitchLessThirty(s)) {
                    byte[] copy = s3Service.read(s);
                    s3Service.upload(copy, parameters.get("outputDir") + "/" + s.replace(inputDir + "/", ""));
                }
            });

            result.put("resultCode", "200");
            result.put("message", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("resultCode", "500");
            result.put("message", e.getLocalizedMessage());
        }
        rabbitProducer.sendMessage(replayTo, correlationKey, result);
    }

    @RabbitListener(queues = FILTER_BY_HEIGHT_QUEUE, concurrency = "1")
    public void receiveFilterByHeight(Message message) {
        String correlationKey = getCorrelationId(message);
        String replayTo = message.getMessageProperties().getReplyTo();
        Map<String, String> result = new HashMap<>();
        try {
            Map<String, String> parameters = readValue(message);
            String inputDir = parameters.get("inputDir");
            List<String> keys = s3Service.getKeys(inputDir);
            keys.stream().forEach(s -> {
                if (s3Service.isHeightLessThirteen(s)) {
                    byte[] copy = s3Service.read(s);
                    s3Service.upload(copy, parameters.get("outputDir") + "/" + s.replace(inputDir + "/", ""));
                }
            });

            result.put("resultCode", "200");
            result.put("message", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("resultCode", "500");
            result.put("message", e.getLocalizedMessage());
        }
        rabbitProducer.sendMessage(replayTo, correlationKey, result);
    }

    @RabbitListener(queues = FILTER_BY_COUNT_QUEUE, concurrency = "1")
    public void receiveFilterByCount(Message message) {
        String correlationKey = getCorrelationId(message);
        String replayTo = message.getMessageProperties().getReplyTo();
        Map<String, String> result = new HashMap<>();
        try {
            Map<String, String> parameters = readValue(message);
            String inputDir = parameters.get("inputDir");
            List<String> keys = s3Service.getKeys(inputDir);
            Collections.shuffle(keys);
            List<String> limitedKeys = keys.stream()
                    .limit(PAGE_LIMIT)
                    .collect(Collectors.toList());
            limitedKeys.forEach(s -> {
                byte[] copy = s3Service.read(s);
                s3Service.upload(copy, parameters.get("outputDir") + "/" + s.replace(inputDir + "/", ""));
            });

            result.put("resultCode", "200");
            result.put("message", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("resultCode", "500");
            result.put("message", e.getLocalizedMessage());
        }
        rabbitProducer.sendMessage(replayTo, correlationKey, result);
    }

    @RabbitListener(queues = GENERATE_PARAMS_QUEUE, concurrency = "1")
    public void generateParams(Message message) {
        String correlationKey = getCorrelationId(message);
        String replayTo = message.getMessageProperties().getReplyTo();
        Map<String, String> result = new HashMap<>();
        try {
            Map<String, String> parameters = readValue(message);
            String inputDir = parameters.get("inputDir");
            List<String> keys = s3Service.getKeys(inputDir);
            ResultDto resultDto = new ResultDto();
            keys.forEach(s -> {
                if (!isSubDirectory(s, inputDir)){
                    System.out.println(s);
                    resultDto.getImages().add(s3Service.getImageDto(s));
                }

            });
            System.out.println(resultDto);
            ObjectMapper om = new ObjectMapper();
            byte[] bytes = om.writeValueAsString(resultDto).getBytes();
            s3Service.upload(bytes, parameters.get("outputDir") + "/parameters.json");

            result.put("resultCode", "200");
            result.put("message", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("resultCode", "500");
            result.put("message", e.getLocalizedMessage());
        }
        rabbitProducer.sendMessage(replayTo, correlationKey, result);
    }

    public static Boolean isSubDirectory(String key, String targetDirectory) {
        return key.replace(targetDirectory + "/", "").contains("/");
    }

    @RabbitListener(queues = DETECT_ANTENNAS_QUEUE, concurrency = "1")
    public void receiveDetect(Message message) {
        long startTime = System.currentTimeMillis();
        HashMap result = new HashMap<String, String>();
        String correlationKey = getCorrelationId(message);
        String replayTo = message.getMessageProperties().getReplyTo();
        try {
            Map<String, String> parameters = readValue(message);
            log.info(">>correlationKey:" + correlationKey + ", replayTo:" + replayTo + ", parameters" + parameters);
            commandLineRunner.run(ANTENNAS_SRC, parameters);
            log.info(">>processed in: {}", System.currentTimeMillis() - startTime);
            result.put("resultCode", "200");
            result.put("message", "OK");
        } catch (Exception ex) {
            ex.printStackTrace();
            result.put("resultCode", "500");
            result.put("message", ex.getLocalizedMessage());
        }
        rabbitProducer.sendMessage(replayTo, correlationKey, result);
    }

    @RabbitListener(queues = COMPRESSOR_QUEUE, concurrency = "1")
    public void receiveCompressor(Message message) {
        long startTime = System.currentTimeMillis();
        HashMap result = new HashMap<String, String>();
        String correlationKey = getCorrelationId(message);
        String replayTo = message.getMessageProperties().getReplyTo();
        try {
            Map<String, String> parameters = readValue(message);
            log.info(">>correlationKey:" + correlationKey + ", replayTo:" + replayTo + ", parameters" + parameters);
            commandLineRunner.run(COMPRESSOR_SRC, parameters);
            log.info(">>processed in: {}", System.currentTimeMillis() - startTime);
            result.put("resultCode", "200");
            result.put("message", "OK");
        } catch (Exception ex) {
            ex.printStackTrace();
            result.put("resultCode", "500");
            result.put("message", ex.getLocalizedMessage());
        }
        rabbitProducer.sendMessage(replayTo, correlationKey, result);
    }

    @RabbitListener(queues = CORROSION_QUEUE, concurrency = "1")
    public void receiveCorrosion(Message message) {
        long startTime = System.currentTimeMillis();
        HashMap result = new HashMap<String, String>();
        String correlationKey = getCorrelationId(message);
        String replayTo = message.getMessageProperties().getReplyTo();
        try {
            Map<String, String> parameters = readValue(message);
            log.info(">>correlationKey:" + correlationKey + ", replayTo:" + replayTo + ", parameters" + parameters);
            commandLineRunner.run(CORROSION_SRC, parameters);
            log.info(">>processed in: {}", System.currentTimeMillis() - startTime);
            result.put("resultCode", "200");
            result.put("message", "OK");
        } catch (Exception ex) {
            ex.printStackTrace();
            result.put("resultCode", "500");
            result.put("message", ex.getLocalizedMessage());
        }
        rabbitProducer.sendMessage(replayTo, correlationKey, result);
    }

    @RabbitListener(queues = LED_QUEUE, concurrency = "1")
    public void receiveLed(Message message) {
        long startTime = System.currentTimeMillis();
        HashMap result = new HashMap<String, String>();
        String correlationKey = getCorrelationId(message);
        String replayTo = message.getMessageProperties().getReplyTo();
        try {
            Map<String, String> parameters = readValue(message);
            log.info(">>correlationKey:" + correlationKey + ", replayTo:" + replayTo + ", parameters" + parameters);
            commandLineRunner.run(LED_SRC, parameters);
            log.info(">>processed in: {}", System.currentTimeMillis() - startTime);
            result.put("resultCode", "200");
            result.put("message", "OK");
        } catch (Exception ex) {
            ex.printStackTrace();
            result.put("resultCode", "500");
            result.put("message", ex.getLocalizedMessage());
        }
        rabbitProducer.sendMessage(replayTo, correlationKey, result);
    }

    @RabbitListener(queues = FLANGES_QUEUE, concurrency = "1")
    public void receiveFlanges(Message message) {
        long startTime = System.currentTimeMillis();
        HashMap result = new HashMap<String, String>();
        String correlationKey = getCorrelationId(message);
        String replayTo = message.getMessageProperties().getReplyTo();
        try {
            Map<String, String> parameters = readValue(message);
            log.info(">>correlationKey:" + correlationKey + ", replayTo:" + replayTo + ", parameters" + parameters);
            commandLineRunner.run(FLANGES_SRC, parameters);
            log.info(">>processed in: {}", System.currentTimeMillis() - startTime);
            result.put("resultCode", "200");
            result.put("message", "OK");
        } catch (Exception ex) {
            ex.printStackTrace();
            result.put("resultCode", "500");
            result.put("message", ex.getLocalizedMessage());
        }
        rabbitProducer.sendMessage(replayTo, correlationKey, result);
    }


    @RabbitListener(queues = MOVE_REPORT_QUEUE, concurrency = "1")
    public void moveReport(Message message) {
        long startTime = System.currentTimeMillis();
        HashMap result = new HashMap<String, String>();
        String correlationKey = getCorrelationId(message);
        String replayTo = message.getMessageProperties().getReplyTo();
        try {
            Map<String, String> parameters = readValue(message);
            log.info(">>correlationKey:" + correlationKey + ", replayTo:" + replayTo + ", parameters" + parameters);
            commandLineRunner.run(MOVE_SRC + " --inputDir " + parameters.get("inputDir").replace("/null", "") + " --outputDir " + parameters.get("outputDir").replace("/null", ""));
            log.info(">>processed in: {}", System.currentTimeMillis() - startTime);
            result.put("resultCode", "200");
            result.put("message", "OK");
        } catch (Exception ex) {
            ex.printStackTrace();
            result.put("resultCode", "500");
            result.put("message", ex.getLocalizedMessage());
        }
        rabbitProducer.sendMessage(replayTo, correlationKey, result);
    }

    private String getCorrelationId(Message message) {
        return message.getMessageProperties().getCorrelationId();
    }

    private HashMap<String, String> readValue(Message message) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(message.getBody(), HashMap.class);
    }
}
