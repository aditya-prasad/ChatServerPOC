package com.clanout.chatserver_poc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.HashMap;
import java.util.Map;

public class ChatRoomService
{
    private static final String TAG = "ChatRoomService";

    private Map<String, ChannelGroup> activeChatRooms;
    private Map<String, String> chatRoomMembers;

    public ChatRoomService()
    {
        activeChatRooms = new HashMap<>();
        chatRoomMembers = new HashMap<>();
    }

    public void joinRoom(String roomId, Channel channel)
    {
        if (!activeChatRooms.containsKey(roomId))
        {
            synchronized (TAG)
            {
                activeChatRooms.put(roomId, new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
            }
        }

        String id = channel.id().asShortText();
        if (chatRoomMembers.containsKey(id))
        {
            throw new IllegalStateException();
        }

        chatRoomMembers.put(id, roomId);
        activeChatRooms.get(roomId).add(channel);
    }

    public String getActiveRoomId(Channel channel)
    {
        String id = channel.id().asShortText();
        return chatRoomMembers.get(id);
    }

    public void leaveRoom(String roomId, Channel channel)
    {
        ChannelGroup chatRoom = activeChatRooms.get(roomId);
        if (chatRoom == null)
        {
            return;
        }

        chatRoomMembers.remove(channel.id().asShortText());
        chatRoom.remove(channel);

        if (chatRoom.isEmpty())
        {
            synchronized (TAG)
            {
                activeChatRooms.remove(roomId);
            }
        }
    }

    public void post(String roomId, Channel sender, String message)
    {
        ChannelGroup chatRoom = activeChatRooms.get(roomId);
        if (chatRoom == null)
        {
            System.out.println("Invalid chat room");
            return;
        }

        String meMessage = "[ME] " + message;
        String otherMessage = "[" + sender.id().asShortText() + "] " + message;

        for (Channel member : chatRoom)
        {
            ByteBuf out = member.alloc().buffer();

            if (member.id().equals(sender.id()))
            {
                out.writeBytes(meMessage.getBytes());
                member.writeAndFlush(out);
            }
            else
            {
                out.writeBytes(otherMessage.getBytes());
                member.writeAndFlush(out);
            }
        }
    }

    public void postSystemMessage(String roomId, String message)
    {
        ChannelGroup chatRoom = activeChatRooms.get(roomId);
        if (chatRoom == null)
        {
            System.out.println("Invalid chat room");
            return;
        }

        message = "[SYSTEM] " + message;

        for (Channel member : chatRoom)
        {
            ByteBuf out = member.alloc().buffer();
            out.writeBytes(message.getBytes());
            member.writeAndFlush(out);
        }
    }
}
