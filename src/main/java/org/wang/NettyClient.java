package org.wang;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;

public class NettyClient {

    private ChatClientHandler clientHandler;
    private String host;
    private int port;

    public NettyClient(String nickName){
        this.clientHandler = new ChatClientHandler(nickName);
    }

    public void connect(String host,int port){
        this.host = host;
        this.port = port;

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {

//                    ch.pipeline().addLast(new IMDecoder());
//                    ch.pipeline().addLast(new IMEncoder());
                    ch.pipeline().addLast(clientHandler);


                }
            });
            ChannelFuture f = b.connect(this.host, this.port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }


    public static void main(String[] args) throws IOException {
        new NettyClient("wanghe").connect("127.0.0.1",8080);

        String url = "http://localhost:8080/images/a.png";
        System.out.println(url.toLowerCase().matches(".*\\.(gif|png|jpg)$"));

    }


}

class ChatClientHandler extends SimpleChannelInboundHandler{

    ChatClientHandler(String nickName){
        super();
    }

    /**
     * tcp链路建立成功后调用
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {

    }
}
