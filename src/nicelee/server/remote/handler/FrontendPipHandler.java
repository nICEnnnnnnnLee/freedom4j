package nicelee.server.remote.handler;

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
	private ByteBuf buf;

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
		if (outboundChannel != null && outboundChannel.isActive()) {
//			System.out.println(" -- channelRead --");
			if (buf != null && buf.readableBytes() > 0) {
				outboundChannel.write(buf);
				buf = null;
			}
			outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) {
					if (future.isSuccess()) {
						ctx.channel().read();
					} else {
						future.channel().close();
					}
				}
			});
		} else {
			ByteBuf temp = (ByteBuf) msg;
//			buf.writeBytes(temp);
			System.err.println("不应该接收到的数据: " + temp.readableBytes());
			temp.release();
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof Channel && outboundChannel == null) {
			outboundChannel = (Channel) evt;
			ctx.channel().read();
		} else if (evt instanceof byte[]) {
			byte[] temp = (byte[]) evt;
			buf = Unpooled.copiedBuffer(temp);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (buf != null) {
			buf.release();
		}
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