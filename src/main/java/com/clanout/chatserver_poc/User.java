package com.clanout.chatserver_poc;

public interface User
{
    String getUserId();

    void onMessageReceived(ChatMessage message);
}
