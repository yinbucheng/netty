package cn.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.TimeUnit;

/**
 * @author ：yinchong
 * @create ：2019/8/5 11:53
 * @description：
 * @modified By：
 * @version:
 */
@SuppressWarnings("all")
public class ClientDemoTest {

    public static void main(String[] args) {
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_SNDBUF, 1024 * 1024 * 10);
        bootstrap.option(ChannelOption.SO_RCVBUF, 1024 * 1024 * 10);
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                pipeline.addLast(new IdleStateHandler(-1, 10, -1, TimeUnit.SECONDS));
                pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0, 4));
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                        System.out.println("get message from server,content:" + msg);
                    }

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        System.out.println("connect to server success");
                        ctx.pipeline().writeAndFlush("hello i am client");
                    }

                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                        IdleStateEvent event = (IdleStateEvent) evt;
                        switch (event.state()){
                            case WRITER_IDLE:
                                ctx.pipeline().writeAndFlush("i am ping from clint");
                        }
                    }
                });

                pipeline.addFirst(new StringEncoder());
                pipeline.addFirst(new LengthFieldPrepender(4));
            }
        });

        try {
            ChannelFuture future = bootstrap.connect("localhost", 9292).sync();
            future.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        System.out.println("启动成功");
                    }
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
