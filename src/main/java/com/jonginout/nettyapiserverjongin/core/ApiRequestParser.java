package com.jonginout.nettyapiserverjongin.core;

import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * InboundHandler를 상속받고 있으며 이벤트 메서드가 실행될 때 FullHttpMessage를 데이터로 받는다.
 */
public class ApiRequestParser extends SimpleChannelInboundHandler<FullHttpMessage> {
    /**
     * log4j logger
     */
    private static final Logger logger = LogManager.getLogger(ApiRequestParser.class);

    private HttpRequest request;

    private JsonObject apiResult;

    private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); // Disk

    // 사용자가 전송한 HTTP 요청의 본문을 추출할 디코더를 멤버 변수로 등록
    private HttpPostRequestDecoder decoder;

    private Map<String, String> reqData = new HashMap<String, String>();

    private static final Set<String> usingHeader = new HashSet<String>();

    static {
        usingHeader.add("token");
        usingHeader.add("email");
        usingHeader.add("test");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        logger.info("요청 처리 완료");
        /**
         * channelRead0 이벤트 메서드의 수행이 완료된 이후에 channelReadComplete가 호출되고
         * 이때 채널 버퍼의 내용을 클라이언트로 전송한다.
         */
        ctx.flush();
    }

    /**
     * 클라리언트가 전송한 데이터가 채널 파이프라인의 모든 디코더를 거치고 난 뒤에 호출된다.
     * 메서드 호출에 입력되는 객체는 FullHttpMessage 인터페이스의 구현체고 HTTP 프로토콜의 모든 데이터가 포함되어있다.
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) {
        // Request header 처리.
        // FullHttpMessage는 HttpRequest, HttpMessage, HttpContent의 최상위 객체
        if (msg instanceof HttpRequest) {
            this.request = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }

            // 헤더 정보 추출
            HttpHeaders headers = request.headers();
            if (!headers.isEmpty()) {
                for (Map.Entry<String, String> h : headers) {
                    String key = h.getKey();
                    if (usingHeader.contains(key)) {
                        reqData.put(key, h.getValue());
                    }
                }
            }

            reqData.put("REQUEST_URI", request.getUri());
            reqData.put("REQUEST_METHOD", request.getMethod().name());
        }

        // Request content 처리.
        if (msg instanceof HttpContent) {
            /**
             * HttpContent의 상위 인터페이스인 LastHttpContent는 모든 HTTP가 디코딩되었고
             * HTTP 프로토콜의 마지막 데이터임을 알리는 인터페이스
             */
            if (msg instanceof LastHttpContent) {
                logger.debug("LastHttpContent message received!!" + request.getUri());

                LastHttpContent trailer = (LastHttpContent) msg;

                // POST 데이터 추출
                readPostData();

                /**
                 * HTTP 프로토콜에서 필요한 데이터의 추출이 완료되면 reqData 맵을 ServiceDispatcher 클래스의
                 * dispatch 메서드를 호출하여 HTTP 요청에 맞는 API 서비스 클래스를 생성한다.
                 */
                ApiRequest service = ServiceDispatcher.dispatch(reqData);

                try {
                    // ServiceDispatcher 클래스의 dispatch 메서드로부터 생성된 API 서비스 클래스를 실행한다.
                    service.executeService();

                    // 결과
                    apiResult = service.getApiResult();
                } finally {
                    reqData.clear();
                }

                // apiResult 멤버 변수에 저장된 API 처리 결과를 클라이언트 채널의 송신 버퍼에 기록한다.
                if (!writeResponse(trailer, ctx)) {
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
                reset();
            }
        }
    }

    private void reset() {
        request = null;
    }

    private void readPostData() {
        try {
            decoder = new HttpPostRequestDecoder(factory, request);
            for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
                if (HttpDataType.Attribute == data.getHttpDataType()) {
                    try {
                        Attribute attribute = (Attribute) data;
                        reqData.put(attribute.getName(), attribute.getValue());
                    } catch (IOException e) {
                        logger.error("BODY Attribute: " + data.getHttpDataType().name(), e);
                        return;
                    }
                } else {
                    logger.info("BODY data : " + data.getHttpDataType().name() + ": " + data);
                }
            }
        } catch (ErrorDataDecoderException e) {
            logger.error(e);
        } finally {
            if (decoder != null) {
                decoder.destroy();
            }
        }
    }

    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpHeaders.isKeepAlive(request);
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST, Unpooled.copiedBuffer(
                apiResult.toString(), CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // -
            // http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Write the response.
        ctx.write(response);

        return keepAlive;
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause);
        ctx.close();
    }
}
