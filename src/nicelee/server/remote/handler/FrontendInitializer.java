package nicelee.server.remote.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;

public class FrontendInitializer extends ChannelInitializer<SocketChannel> {

	final SslContext sslCtx;

	public FrontendInitializer(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline pipeline = ch.pipeline();
		if (sslCtx != null) {
			pipeline.addLast(sslCtx.newHandler(ch.alloc()));
		}
//		pipeline.addLast("log", new LoggingHandler(LogLevel.INFO));
//		pipeline.addLast("http_request_decode", new HttpRequestDecoder());
//		pipeline.addLast("http_response_encode", new HttpResponseEncoder());
		pipeline.addLast("auth", new FrontendAuthHandler());
		pipeline.addLast("pip", new FrontendPipHandler());
	}
}