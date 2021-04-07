package nicelee.server.local.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class FrontendInitializer extends ChannelInitializer<SocketChannel> {

	public FrontendInitializer() {
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline pipeline = ch.pipeline();
//		pipeline.addLast("log", new LoggingHandler(LogLevel.INFO));
		pipeline.addLast("socks5", new FrontendSocks5Handler());
		pipeline.addLast("pip", new FrontendPipHandler());
	}
}