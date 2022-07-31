package com.jonginout.nettyapiserverjongin;

import com.jonginout.nettyapiserverjongin.core.ApiRequestParser;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;

/**
 * 네티로 구현한 API 서버에서 제일 중요한 부분인 채널 파이프라인 설정코드!
 */
public class ApiServerInitializer extends ChannelInitializer<SocketChannel> {
    private final SslContext sslCtx;

    // ssl 컨텍트스트
    public ApiServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        // 클라이언트 채널로 수신된 HTTP 데이터를 처리하기 위한 채널 파이프라인 객체
        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }

        //// 디코더 ///////

        /**
         * HttpRequestDecoder는 HTTP 요청을 처리하는 디코더,
         * 즉 클라이언트가 전송한 HTTP 프로토콜을 네이트이 바이트 버퍼로 변환하는 작업 수행
         */
        p.addLast(new HttpRequestDecoder());
        /**
         * HttpObjectAggregator는 HTTP 프로토콜에서 발생하는 메시지 파편화를 처리하는 디코더.
         * HTTP 프로토콜을 구성하는 데이터가 나위어서 수신되었을때 데이터를 하나로 합쳐주는 역할 수행,
         * 인자로 입력된 65536은 한꺼번에 처리가 가능한 최대 데이터 크기다.
         * 65Kbyte 이상의 데이터가 하나의 HTTP 요청으로 수신되면 TooLongFrameException 예외가 발생한다.
         */
        p.addLast(new HttpObjectAggregator(65536));

        //// 인코더 ///////

        /**
         * HttpResponseEncoder는 수신된 HTTP 요청의 처리 결과를 클라이언트[로] 전송할때 HTTP 프로토콜로 변환해주는 인코더!
         */
        p.addLast(new HttpResponseEncoder());
        /**
         * HttpContentCompressor는 HTTP 프로토콜로 송수신되는 HTTP의 본문 데이터를 gzip압축 알고리즘을 사용하여 압축과 압축 해제를 수행한다.
         * 즉 HttpContentCompressor는 ChannelDuplexHandler 클래스를 상속받기 때문에 인바운드와 아웃바운드에서 모두 호출된다.
         */
        p.addLast(new HttpContentCompressor());
        /**
         * ApiRequestParsers는 클라이언트로부터 수신된 HTTP 데이터에서 헤더와 데이터 값을 추출하여
         * 토큰 발급과 같은 업무 처리 클래스로 분기하는 클래스로써 API 서버의 컨트롤러 역할을 수행한다.
         */
        p.addLast(new ApiRequestParser());

        /**
         * [클라이언트로부터 데이터를 수신했을때 데이터 호출 순서]
         * HttpRequestDecoder -> HttpObjectAggregator -> HttpResponseEncoder -> HttpContentCompressor -> ApiRequestParser
         * 순서로 실행된다.
         *
         * [ApiRequestParser의 처리가 완료되어 채널로 데이터를 기록할 때 호출되는 순서]
         * ApiRequestParser -> HttpContentCompressor -> HttpResponseEncoder
         */
    }
}
