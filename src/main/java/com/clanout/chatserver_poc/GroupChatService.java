package com.clanout.chatserver_poc;

import io.netty.util.internal.ConcurrentSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GroupChatService
{
    private static final String TAG = "GroupChatService";

    private ConcurrentMap<String, ConcurrentSet<User>> activeChatRooms;
    private ConcurrentMap<String, String> chatRoomMembers;

    public GroupChatService()
    {
        activeChatRooms = new ConcurrentHashMap<>();
        chatRoomMembers = new ConcurrentHashMap<>();
    }

    public void joinRoom(String roomId, User user)
    {
        activeChatRooms.putIfAbsent(roomId, new ConcurrentSet<>());

        String userId = user.getUserId();
        if (chatRoomMembers.containsKey(userId))
        {
            throw new IllegalStateException();
        }

        chatRoomMembers.put(userId, roomId);
        activeChatRooms.get(roomId).add(user);
        post(ChatMessages.buildMemberJoinedMessage(roomId, userId));
    }

    public String getActiveRoomId(User user)
    {
        return chatRoomMembers.get(user.getUserId());
    }

    public void leaveRoom(String roomId, User user)
    {
        ConcurrentSet<User> chatRoom = activeChatRooms.get(roomId);
        if (chatRoom == null)
        {
            return;
        }

        post(ChatMessages.buildMemberLeftMessage(roomId, user.getUserId()));

        chatRoomMembers.remove(user.getUserId());
        chatRoom.remove(user);

        if (chatRoom.isEmpty())
        {
            activeChatRooms.remove(roomId);
        }
    }

    public void post(ChatMessage chatMessage)
    {
        ConcurrentSet<User> chatRoom = activeChatRooms.get(chatMessage.getRoomId());
        if (chatRoom == null)
        {
            System.out.println("Invalid chat room");
            return;
        }

        for (User member : chatRoom)
        {
            member.onMessageReceived(chatMessage);
        }
    }
}
