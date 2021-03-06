package service.messages;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

public class ChatMessageReceive implements MySerializable {
    @Getter
    @Setter
    private String SenderName;
    @Getter
    @Setter
    private Timestamp SentTime;
    @Getter
    @Setter
    private long UniqueId;
    @Getter
    @Setter
    private String Message;

    public ChatMessageReceive(){}
    public ChatMessageReceive(String senderName, Timestamp sentTime, long uniqueId, String message) {
        SenderName = senderName;
        SentTime = sentTime;
        UniqueId = uniqueId;
        Message = message;
    }
}
