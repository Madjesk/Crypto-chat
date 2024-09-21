package ru.mai.javachatservice.model.client;

import lombok.Builder;
import org.springframework.data.redis.core.RedisHash;
import ru.mai.javachatservice.model.messages.Message;

import java.io.Serializable;

@Builder
@RedisHash("MessageInfo")
public class MessageInfo implements Serializable {
    private String id;
    private long from;
    private long to;
    private Message message;
}
