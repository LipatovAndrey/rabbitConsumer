package io.airspector.consumer.message;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;


@Slf4j
@Service
public class RabbitProducer {

    public static final String EXCHANGE = "";

    @Autowired
    private ConnectionFactory connectionFactory;


    public void sendMessage(String routingKey, String correlationId, Map<String, String> payloads) {
        log.info(">> sendMessage correlationKey:" + correlationId + ", replayTo:" + routingKey + ", payloads" + payloads);
        try (Connection connection = connectionFactory.createConnection();
             Channel channel = connection.createChannel(false)) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(correlationId)
                    .build();
            channel.basicPublish(EXCHANGE, routingKey, props, objectMapper.writeValueAsBytes(payloads));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
