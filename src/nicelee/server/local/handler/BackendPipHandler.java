package nicelee.server.local.handler;

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
//	   System.out.println("-- Local BackendPipHandler channelActive--");
		ctx.read(); // 指示BackEnd Channel 读取内容
//		inboundChannel.read(); // 指示FrontEnd Channel 读取内容
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
//	   System.out.println("-- BackendPipHandler channelRead--");
//		ByteBuf buf = (ByteBuf) msg;
//		System.err.println(CommonMethods.ByteBufToString(buf));
		inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) {
				if (future.isSuccess()) {
//            	   System.out.println("-- BackendPipHandler inboundChannel writeAndFlush isSuccess--");
					ctx.channel().read(); // 指示BackEnd Channel 读取内容
				} else {
//            	   System.out.println("-- BackendPipHandler inboundChannel writeAndFlush fail--");
					future.channel().close();
				}
			}
		});
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
//		System.out.println("========channelInactive===================");
		FrontendPipHandler.closeOnFlush(inboundChannel);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (!(cause instanceof IOException))
			cause.printStackTrace();
		FrontendPipHandler.closeOnFlush(ctx.channel());
	}
}