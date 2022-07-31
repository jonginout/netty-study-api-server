package com.jonginout.nettyapiserverjongin;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;

@Component
public final class ApiServer {
    /**
     * InetSocketAddress 객체는 API 서버의 서비스 포트인 8080을 사용하며 이 값은 프로퍼티에 지정되어 있다.
     * 즉 api-server.properties 파일의 tcp.port 값을 바꾸면 API 서버의 서비스 포트도 변경된다.
     */
    @Autowired
    @Qualifier("tcpSocketAddress")
    private InetSocketAddress address;

    @Autowired
    @Qualifier("workerThreadCount")
    private int workerThreadCount;

    @Autowired
    @Qualifier("bossThreadCount")
    private int bossThreadCount;

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreadCount);
        EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreadCount);
        ChannelFuture channelFuture;

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    /**
                     * API 서버의 채널 파이프라인 설정 클래스를 지정한다.
                     * ApiServerInitializer의 인자는 SSL 컨텍스트인데 우선 null
                     */
                    .childHandler(new ApiServerInitializer(null));

            Channel ch = b.bind(address).sync().channel();

            channelFuture = ch.closeFuture();

            /**
             * sync 메서드를 호출하면 코드가 블로킹되어 이후의 코드가 실행되지 않으므로 주석,
             * 이 부분이 이전에 작성한 코드의 마지막 부분이며 뒤에 새로운 부트스트랩을 추가
             */
            // channelFuture.sync();

            final SslContext sslContext;
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslContext = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());

            // 새로운 부트스트랩 추가
            ServerBootstrap b2 = new ServerBootstrap();
            // 이벤트 루프는 첫 번째 부트스트랩과 공유하여 사용하도록 함
            b2.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    /**
                     * SSL 연결을 지원하려면 SelfSignedCertificate 클래스 객체를 사용함
                     */
                    .childHandler(new ApiServerInitializer(sslContext));

            Channel ch2 = b2.bind(8443).sync().channel();

            channelFuture = ch2.closeFuture();
            channelFuture.sync();
        } catch (InterruptedException | CertificateException | SSLException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
