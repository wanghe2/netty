package org.wang;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;

/**
 * Hello world!
 *
 */
public class NettyServer
{

    private int port = 8080;
    public void start(int port){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {

                            ChannelPipeline pipeline = ch.pipeline();

                            /** 解析自定义协议 */
//                            pipeline.addLast(new IMDecoder());  //Inbound
//                            pipeline.addLast(new IMEncoder());  //Outbound
//                            pipeline.addLast(new TerminalServerHandler());  //Inbound

                            /** 解析Http请求 */
                            pipeline.addLast(new HttpServerCodec());  //Outbound
                            //主要是将同一个http请求或响应的多个消息对象变成一个 fullHttpRequest完整的消息对象
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));//Inbound
                            //主要用于处理大数据流,比如一个1G大小的文件如果你直接传输肯定会撑暴jvm内存的 ,加上这个handler我们就不用考虑这个问题了
                            pipeline.addLast(new ChunkedWriteHandler());//Inbound、Outbound
//                            pipeline.addLast(new HttpServerHandler());//Inbound

//                            /** 解析WebSocket请求 */
//                            pipeline.addLast(new WebSocketServerProtocolHandler("/im"));    //Inbound
//                            pipeline.addLast(new WebSocketServerHandler()); //Inbound

                        }
                    });
            ChannelFuture f = b.bind(this.port).sync();
            System.out.println("服务已启动,监听端口" + this.port);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void start(){
        start(this.port);
    }

    public static void main( String[] args )
    {
        new NettyServer().start();
    }
}


 class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    //获取class路径
    private URL baseURL = HttpServerHandler.class.getResource("");
    private final String webroot = "webroot";

    private File getResource(String fileName) throws Exception{
        String basePath = baseURL.toURI().toString();
        int start = basePath.indexOf("classes/");
        basePath = (basePath.substring(0,start) + "/" + "classes/").replaceAll("/+","/");

        String path = basePath + webroot + "/" + fileName;
//        log.info("BaseURL:" + basePath);
        path = !path.contains("file:") ? path : path.substring(5);
        path = path.replaceAll("//", "/");
        return new File(path);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.getUri();

        RandomAccessFile file = null;
        try{
            String page = uri.equals("/") ? "chat.html" : uri;
            file =	new RandomAccessFile(getResource(page), "r");
        }catch(Exception e){
            ctx.fireChannelRead(request.retain());
            return;
        }

        HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
        String contextType = "text/html;";
        if(uri.endsWith(".css")){
            contextType = "text/css;";
        }else if(uri.endsWith(".js")){
            contextType = "text/javascript;";
        }else if(uri.toLowerCase().matches(".*\\.(jpg|png|gif)$")){
            String ext = uri.substring(uri.lastIndexOf("."));
            contextType = "image/" + ext;
        }
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, contextType + "charset=utf-8;");

        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        if (keepAlive) {
            response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length());
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        ctx.write(response);

        ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));

        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

        file.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        Channel client = ctx.channel();
        System.err.println("Client:"+client.remoteAddress()+"异常");
        // 当出现异常就关闭连接
        cause.printStackTrace();
        ctx.close();
    }
}
