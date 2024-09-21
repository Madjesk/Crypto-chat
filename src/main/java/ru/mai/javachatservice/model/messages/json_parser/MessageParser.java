package ru.mai.javachatservice.model.messages.json_parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ru.mai.javachatservice.model.messages.Message;

import java.util.Arrays;

@Slf4j
public class MessageParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MessageParser() {}

    public static Message parseMessage(String message) {
        try {
            return OBJECT_MAPPER.readValue(message, Message.class);
        } catch (JsonProcessingException ex) {
            log.error("Error while parsing json string");
            log.error(Arrays.deepToString(ex.getStackTrace()));
        }

        return null;
    }
}
