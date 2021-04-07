package nicelee.server.remote.handler;

import java.io.IOException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class BackendPipHandler extends ChannelInboundHandlerAdapter {

	private final Channel inboundChannel;

	public BackendPipHandler(Channel inboundChannel) {
		this.inboundChannel = inboundChannel;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.read(); // 指示BackEnd Channel
//		inboundChannel.read();
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
//	   System.out.println("-- BackendPipHandler channelRead--");
		inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) {
				if (future.isSuccess()) {
					ctx.channel().read(); // 指示BackEnd Channel
				} else {
					future.channel().close();
				}
			}
		});
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		FrontendPipHandler.closeOnFlush(inboundChannel);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (!(cause instanceof IOException))
			cause.printStackTrace();
		FrontendPipHandler.closeOnFlush(ctx.channel());
	}
}