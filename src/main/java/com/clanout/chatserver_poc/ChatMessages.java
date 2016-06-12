package com.clanout.chatserver_poc;

public final class ChatMessages
{
    public static ChatMessage buildMemberJoinedMessage(String roomId, String userId)
    {
        return new ChatMessage(roomId, "SYSTEM", userId + " joined\r\n");
    }

    public static ChatMessage buildMemberLeftMessage(String roomId, String userId)
    {
        return new ChatMessage(roomId, "SYSTEM", userId + " left\r\n");
    }

    public static ChatMessage buildRoomAlreadyJoinedMessage()
    {
        return new ChatMessage(null, "SYSTEM", "You have already joined another room. Enter 'leave:' to leave that room then try again\r\n");
    }

    public static ChatMessage buildNoRoomJoinedMessage()
    {
        return new ChatMessage(null, "SYSTEM", "You have not joined any room. Enter 'join:<room_name>' to join that room\r\n");
    }

    private ChatMessages()
    {
    }
}
