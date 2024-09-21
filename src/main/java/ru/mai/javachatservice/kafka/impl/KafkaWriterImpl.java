package ru.mai.javachatservice.kafka.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.stereotype.Service;
import ru.mai.javachatservice.kafka.KafkaWriter;

import java.util.Properties;

@Slf4j
@Service
public class KafkaWriterImpl implements KafkaWriter {
    private static final String bootstrapServer = "localhost:9093";
    private static final  String clientId = "producerKafkaWriter";
    private static final String autoCreateTopics = "true";
    private final KafkaProducer<byte[], byte[]> kafkaProducer;

    public KafkaWriterImpl() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put("auto.create.topics.enable", autoCreateTopics);

        this.kafkaProducer = new KafkaProducer<>(
                props,
                new ByteArraySerializer(),
                new ByteArraySerializer()
        );
    }

    @Override
    public void processing(byte[] messageBytes, String outputTopic) {
        log.info("Sending message to {}...", outputTopic);

        try {
            kafkaProducer.send(new ProducerRecord<>(
                    outputTopic,
                    messageBytes
            ));
        } catch (Exception ex) {
            log.error("Error while sending message");
        }
    }

    @Override
    public void close() {
        kafkaProducer.close();
    }
}