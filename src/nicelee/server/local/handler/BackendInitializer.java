package nicelee.server.local.handler;

import javax.net.ssl.SSLEngine;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import nicelee.server.local.LocalConfig;

public class BackendInitializer extends ChannelInitializer<SocketChannel> {

	final Channel inboundChannel;
	final String host;
	final String port;
	final ChannelHandlerContext ctxOfFrontHandler;
	final boolean proxy;

	public BackendInitializer(Channel inboundChannel, String host, String port) {
		this(inboundChannel, host, port, null);
	}

	// inboundChannel 用于与client端的读写
	// host, port 用于告诉proxy server应该与谁建立连接
	// ctxOfFrontHandler 用来通过userEventTrigger的方式返回数据, 以便进行下一步
	public BackendInitializer(Channel inboundChannel, String host, String port,
			ChannelHandlerContext ctxOfFrontHandler) {
		this.inboundChannel = inboundChannel;
		this.host = host;
		this.port = port;
		this.ctxOfFrontHandler = ctxOfFrontHandler;
		this.proxy = true;
	}

	public BackendInitializer(Channel inboundChannel, ChannelHandlerContext ctxOfFrontHandler) {
		this.inboundChannel = inboundChannel;
		this.ctxOfFrontHandler = ctxOfFrontHandler;
		this.host = this.port = null;
		this.proxy = false;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline pipeline = ch.pipeline();
//		pipeline.addLast("log", new LoggingHandler(LogLevel.INFO));

		if (proxy) {
			if (LocalConfig.sslContext != null) {
				SSLEngine engine = LocalConfig.sslContext.createSSLEngine(LocalConfig.domain, LocalConfig.remotePort);
				engine.setUseClientMode(true);
				ch.pipeline().addLast("ssl", new SslHandler(engine));
			}
			pipeline.addLast("header", new BackendAuthHandler(host, port, ctxOfFrontHandler));
		} else {
			pipeline.addLast("plain", new BackendPlainHandler(ctxOfFrontHandler));
		}

//		pipeline.addLast(new HttpClientCodec());
//		pipeline.addLast(new HttpContentDecompressor());

		pipeline.addLast("pip", new BackendPipHandler(inboundChannel));
	}
}