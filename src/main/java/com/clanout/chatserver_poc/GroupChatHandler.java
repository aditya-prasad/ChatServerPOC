package com.clanout.chatserver_poc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.ref.WeakReference;

public class GroupChatHandler extends ChannelInboundHandlerAdapter implements User
{
    private static Logger LOG = LogManager.getLogger();

    private static final String JOIN_PREFIX = "join:";
    private static final String LEAVE_PREFIX = "leave:";

    private GroupChatService groupChatService;
    private WeakReference<ChannelHandlerContext> context;

    public GroupChatHandler(GroupChatService groupChatService)
    {
        this.groupChatService = groupChatService;
    }

    @Override
    public String getUserId()
    {
        ChannelHandlerContext ctx = context.get();
        if (ctx != null)
        {
            return ctx.channel().id().asShortText();
        }
        else
        {
            return null;
        }
    }

    @Override
    public void onMessageReceived(ChatMessage message)
    {
        String msg = null;
        if (message.getSenderId().equals(getUserId()))
        {
            msg = "[ME] " + message.getMessage();
        }
        else
        {
            msg = "[" + message.getSenderId() + "] " + message.getMessage();
        }

        ChannelHandlerContext ctx = context.get();
        if (ctx != null)
        {
            ByteBuf out = ctx.alloc().buffer();
            out.writeBytes(msg.getBytes());
            ctx.write(out);
            ctx.flush();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        context = new WeakReference<>(ctx);
        LOG.info("Session created for " + getUserId());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        LOG.info("Session closed for " + getUserId());

        String roomId = groupChatService.getActiveRoomId(this);
        if (roomId != null)
        {
            groupChatService.leaveRoom(roomId, this);
        }
        context = null;

        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        ByteBuf in = (ByteBuf) msg;
        try
        {
            String input = in.toString(CharsetUtil.UTF_8);
            if (input.startsWith(JOIN_PREFIX))
            {
                try
                {
                    String roomId = input.split(":")[1];
                    groupChatService.joinRoom(roomId, this);
                }
                catch (IllegalStateException e)
                {
                    onMessageReceived(ChatMessages.buildRoomAlreadyJoinedMessage());
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Unable to join chat room");
                }
            }
            else if (input.startsWith(LEAVE_PREFIX))
            {
                try
                {
                    String roomId = groupChatService.getActiveRoomId(this);
                    if (roomId == null)
                    {
                        onMessageReceived(ChatMessages.buildNoRoomJoinedMessage());
                    }
                    else
                    {
                        groupChatService.leaveRoom(roomId, this);
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Unable to leave room");
                }
            }
            else
            {
                try
                {
                    String roomId = groupChatService.getActiveRoomId(this);
                    if (roomId == null)
                    {
                        onMessageReceived(ChatMessages.buildNoRoomJoinedMessage());
                    }
                    else
                    {
                        ChatMessage chatMessage = new ChatMessage(roomId, getUserId(), input);
                        groupChatService.post(chatMessage);
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Unable to post message");
                }
            }
        }
        finally
        {
            ReferenceCountUtil.release(in);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        LOG.error("Error", cause);
        ctx.close();
    }
}
