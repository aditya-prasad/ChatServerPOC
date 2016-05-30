package com.clanout.chatserver_poc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public class GroupChatHandler extends ChannelHandlerAdapter
{
    private static final String JOIN_PREFIX = "join:";
    private static final String LEAVE_PREFIX = "leave:";

    private ChatRoomService chatRoomService;

    public GroupChatHandler(ChatRoomService chatRoomService)
    {
        super();
        this.chatRoomService = chatRoomService;
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
                    chatRoomService.joinRoom(roomId, ctx.channel());

                    String message = ctx.channel().id().asShortText() + " joined\r\n";
                    chatRoomService.postSystemMessage(roomId, message);
                }
                catch (IllegalStateException e)
                {
                    String message = "You have already joined another room. Enter 'leave:' to leave that room then try again\r\n";
                    ByteBuf out = ctx.alloc().buffer();
                    out.writeBytes(message.getBytes());
                    ctx.writeAndFlush(out);
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
                    String roomId = chatRoomService.getActiveRoomId(ctx.channel());
                    if (roomId == null)
                    {
                        String message = "You have not joined any room. Enter 'join:<room_name>' to join that room\r\n";
                        ByteBuf out = ctx.alloc().buffer();
                        out.writeBytes(message.getBytes());
                        ctx.writeAndFlush(out);
                    }
                    else
                    {
                        String message = ctx.channel().id().asShortText() + " left\r\n";
                        chatRoomService.postSystemMessage(roomId, message);
                        chatRoomService.leaveRoom(roomId, ctx.channel());
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
                    String roomId = chatRoomService.getActiveRoomId(ctx.channel());
                    if (roomId == null)
                    {
                        String message = "You have not joined any room. Enter 'join:<room_name>' to join that room\r\n";
                        ByteBuf out = ctx.alloc().buffer();
                        out.writeBytes(message.getBytes());
                        ctx.writeAndFlush(out);
                    }
                    else
                    {
                        chatRoomService.post(roomId, ctx.channel(), input);
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
        cause.printStackTrace();
        ctx.close();
    }
}
