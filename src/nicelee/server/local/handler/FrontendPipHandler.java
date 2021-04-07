package nicelee.server.local.handler;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class FrontendPipHandler extends ChannelInboundHandlerAdapter {

	private Channel outboundChannel;

	public FrontendPipHandler() {
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
		if (outboundChannel != null && outboundChannel.isActive()) {
//			System.out.println("-- FrontendPipHandler channelRead --");
			outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) {
					if (future.isSuccess()) {
//						System.out.println("-- FrontendPipHandler channelRead outboundChannel writeAndFlush --");
						ctx.channel().read(); // 指示FontEnd Channel 读取内容
//						outboundChannel.read();
					} else {
						future.channel().close();
					}
				}
			});
		} else {
			ByteBuf buf = (ByteBuf) msg;
			System.out.println("收到了不该收到的数据: " + buf.readableBytes());
			buf.release();
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof Channel && outboundChannel == null) {
			outboundChannel = (Channel) evt;
			ctx.channel().read();
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (outboundChannel != null) {
			closeOnFlush(outboundChannel);
		}
		closeOnFlush(ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (!(cause instanceof IOException))
			cause.printStackTrace();
		channelInactive(ctx);
	}

	static void closeOnFlush(Channel ch) {
		if (ch.isActive()) {
			ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}