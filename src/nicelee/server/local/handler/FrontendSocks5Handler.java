package nicelee.server.local.handler;

import java.net.Inet4Address;
import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import nicelee.server.local.LocalConfig;
import nicelee.util.CNIPRecognizer;
import nicelee.util.CommonMethods;

public class FrontendSocks5Handler extends ChannelInboundHandlerAdapter {

	final public static String name = "socks5";
	private int steps = 0;
	private InetSocketAddress localAddr;

	static EventLoopGroup eventGroup;

	public FrontendSocks5Handler() {
		if (eventGroup == null)
			eventGroup = new NioEventLoopGroup();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
//		System.out.println("-- FrontendSocks5Handler: local server has received a connection --");
		ctx.channel().read();
	}

	private void connectToPlainServer(ChannelHandlerContext ctx, String remoteIp, short remotePort) {
//		System.out.println("国内直连：" + remoteIp);
		final Channel inboundChannel = ctx.channel();
		Bootstrap b = new Bootstrap();
//		b.group(inboundChannel.eventLoop()).channel(ctx.channel().getClass())
		b.group(eventGroup).channel(ctx.channel().getClass()).handler(new BackendInitializer(inboundChannel, ctx))
				.option(ChannelOption.AUTO_READ, false);
		ChannelFuture f = b.connect(remoteIp, remotePort);
		f.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) {
				if (future.isSuccess()) {
					ctx.fireUserEventTriggered(future.channel());
//					inboundChannel.read();
				} else {
					inboundChannel.close();
				}
			}
		});
	}

	private void connectToProxyServer(ChannelHandlerContext ctx, String remoteIp, short remotePort) {
//		System.out.printf("proxy转发：%s:%s\n", remoteIp, remotePort);
//		System.out.printf("remote (%s:%s)\n", LocalConfig.remoteHost, LocalConfig.remotePort);
		final Channel inboundChannel = ctx.channel();
		Bootstrap b = new Bootstrap();
		b.group(eventGroup).channel(ctx.channel().getClass())
				.handler(new BackendInitializer(inboundChannel, remoteIp, "" + remotePort, ctx))
				.option(ChannelOption.AUTO_READ, false);
		ChannelFuture f = b.connect(LocalConfig.remoteHost, LocalConfig.remotePort);
		f.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) {
				if (future.isSuccess()) {
					ctx.fireUserEventTriggered(future.channel());
//					inboundChannel.read();
				} else {
					inboundChannel.close();
				}
			}
		});
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof InetSocketAddress) {
//			System.out.println("-- FrontendSocks5Handler: userEventTriggered --");
			localAddr = (InetSocketAddress) evt;
			try {
				ByteBuf newMsg = ctx.alloc().buffer(10);
				byte[] reply = { 0x05, 0x00, 0x00, 0x01 };
				Inet4Address addr = (Inet4Address) localAddr.getAddress();
				byte[] ipBytes = addr.getAddress();
				newMsg.writeBytes(reply);
				newMsg.writeBytes(ipBytes);
				newMsg.writeShort((short) localAddr.getPort());
				ctx.channel().writeAndFlush(newMsg).await();

				ctx.pipeline().remove(this);

//				ctx.channel().read(); // 指示FrontEnd Channel 读取内容(此时指向Pip Handler)
			} catch (InterruptedException e) {
				e.printStackTrace();
				ctx.close();
			}
		} else {
			ctx.fireUserEventTriggered(evt);
		}
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
//		System.out.println("-- FrontendSocks5Handler: local server channelRead --");
//		System.out.println("steps: " + steps);
		Channel channel = ctx.channel();
		ByteBuf buf = (ByteBuf) msg;
//		System.out.println("data size: " + data.readableBytes());
		switch (steps) {
		case 0:
			if (buf.readByte() == 0x05 && buf.readByte() == 0x01 && buf.readByte() == 0x00) {
//				System.out.println("socks5 header received");
				steps = 1;
				byte[] content = { 0x05, 0x00 };
//				ByteBuf newMsg = ctx.alloc().buffer(content.length);
//				newMsg.writeBytes(content);
				buf.writeBytes(content);
				try {
					channel.writeAndFlush(buf).await();
					channel.read();// 指示FrontEnd Channel 读取内容(此时仍然在 Socks5 Handler)
				} catch (InterruptedException e) {
					buf.release();
					ctx.close();
				}
			} else {
				System.out.println("socks5 header not correct");
				buf.release();
				ctx.close();
			}

			break;
		case 1:
			steps = 2;
			byte[] buffer = new byte[4];
			buf.readBytes(buffer);
//			System.out.println("mode: " + buffer[1]);
//			System.out.println("addrtype: " + buffer[3]);
			int addrtype = buffer[3];
			if (buffer[1] == 1) {
				String remoteIp = null;
				if (addrtype == 1) {
					buf.readBytes(buffer);
					remoteIp = CommonMethods.ipBytesToString(buffer);
				} else if (addrtype == 3) {
					int addrLen = buf.readableBytes();
					byte[] domain = new byte[addrLen];
					buf.readBytes(domain);
					remoteIp = new String(domain);
				} else {
					buf.release();
					ctx.close();
					break;
				}
				buf.readBytes(buffer, 0, 2);
				buf.release();
				short remotePort = CommonMethods.readShort(buffer, 0);
//				System.out.println(remoteIp + ": " + remotePort);
				if (LocalConfig.dircectIfCN && addrtype == 1 && CNIPRecognizer.isCNIP(remoteIp)) {
					connectToPlainServer(ctx, remoteIp, remotePort);
				} else {
					connectToProxyServer(ctx, remoteIp, remotePort);
				}
				break;
			}
		default:
			ctx.close();
			buf.release();
			break;
		}

	}

}