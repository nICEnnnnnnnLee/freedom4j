package nicelee.server.remote.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class BackendInitializer extends ChannelInitializer<SocketChannel> {

	final Channel inboundChannel;

	public BackendInitializer(Channel inboundChannel) {
		this.inboundChannel = inboundChannel;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("pip", new BackendPipHandler(inboundChannel));
	}
}