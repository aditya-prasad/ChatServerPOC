package com.clanout.chatserver_poc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class ChatMessage
{
    private String id;
    private String roomId;
    private String senderId;
    private String message;
    private OffsetDateTime timestamp;

    public ChatMessage(String roomId, String senderId, String message)
    {
        id = roomId + "_" + System.nanoTime();
        timestamp = OffsetDateTime.now(ZoneOffset.UTC);

        this.roomId = roomId;
        this.senderId = senderId;
        this.message = message;
    }

    public String getId()
    {
        return id;
    }

    public String getRoomId()
    {
        return roomId;
    }

    public String getSenderId()
    {
        return senderId;
    }

    public String getMessage()
    {
        return message;
    }

    public OffsetDateTime getTimestamp()
    {
        return timestamp;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        ChatMessage that = (ChatMessage) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }
}
