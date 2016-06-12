package com.clanout.chatserver_poc;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadFactory;

public class ChatServer
{
    private static Logger LOG = LogManager.getLogger();

    private int port;

    public ChatServer(int port)
    {
        this.port = port;
    }

    public void run() throws Exception
    {
        GroupChatService groupChatService = new GroupChatService();

        EventLoopGroup selectorGroup = selectorGroup();
        EventLoopGroup workerGroup = workerGroup();

        try
        {
            ServerBootstrap b = new ServerBootstrap();
            b.group(selectorGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception
                        {
                            ch.pipeline().addLast(new GroupChatHandler(groupChatService));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            LOG.info("Server Started..");

            f.channel().closeFuture().sync();
        }
        finally
        {
            workerGroup.shutdownGracefully();
            selectorGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception
    {
        int port = 7777;
        new ChatServer(port).run();
    }

    private static EventLoopGroup workerGroup()
    {
        String THREAD_NAME_FORMAT = "worker-%d";
        int WORKER_THREAD_POOL_SIZE = 3;
        ThreadFactory workerThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat(THREAD_NAME_FORMAT)
                .build();
        return new NioEventLoopGroup(WORKER_THREAD_POOL_SIZE, workerThreadFactory);
    }

    private static EventLoopGroup selectorGroup()
    {
        String THREAD_NAME_FORMAT = "selector-%d";
        ThreadFactory selectorThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat(THREAD_NAME_FORMAT)
                .build();
        return new NioEventLoopGroup(1, selectorThreadFactory);
    }
}
