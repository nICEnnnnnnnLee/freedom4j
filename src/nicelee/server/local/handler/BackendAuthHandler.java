package nicelee.server.local.handler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import nicelee.server.local.LocalConfig;
import nicelee.util.CommonMethods;

public class BackendAuthHandler extends ChannelInboundHandlerAdapter {

	private StringBuffer header;
	final String host;
	final String port;
	final ChannelHandlerContext ctxOfFrontHandler;

	public BackendAuthHandler(String host, String port, ChannelHandlerContext ctx) {
		header = new StringBuffer();
		this.host = host;
		this.port = port;
		this.ctxOfFrontHandler = ctx;
	}

	public BackendAuthHandler(String host, String port) {
		this(host, port, null);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx0) {
		final ChannelHandlerContext ctx = ctx0;
//		System.out.println("-- BackendAuthHandler channelActive--");
		HashMap<String, String> newCookie = new HashMap<>(LocalConfig.cookies);
		String currentTime = String.valueOf(System.currentTimeMillis());
		String token = new StringBuilder(LocalConfig.password).append(LocalConfig.salt).append(currentTime).toString();
		newCookie.put("my_domain", host);
		newCookie.put("my_port", port);
		newCookie.put("my_time", currentTime);
		newCookie.put("my_token", CommonMethods.MD5(token));
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("GET %s HTTP/%s\r\n", LocalConfig.path, LocalConfig.http_version))
				.append(String.format("Host: %s:%s\r\n", LocalConfig.domain, LocalConfig.port))
				.append(String.format("User-Agent: %s\r\n", LocalConfig.userAgent, LocalConfig.http_version))
				.append("Accept: */*\r\n")
				.append("Accept-Language: zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2\r\n")
				.append("Sec-WebSocket-Version: 13\r\n").append("Sec-WebSocket-Extensions: permessage-deflate\r\n")
				.append(String.format("Origin: https://%s\r\n", LocalConfig.domain)).append("Upgrade: websocket\r\n")
				.append("Connection: keep-alive, Upgrade\r\n").append("Pragma: no-cache\r\n")
				.append("Cache-Control: no-cache\r\n")
				.append(String.format("Origin: https://%s\r\n", LocalConfig.domain))
				.append(String.format("Cookie: %s\r\n", genCookie(newCookie))).append("Sec-WebSocket-Key: ")
				.append(CommonMethods.getRandomString(24)).append("\r\n\r\n");
//		System.out.println(sb.toString());
		byte content[] = sb.toString().getBytes();
		ByteBuf newMsg = ctx.alloc().buffer(content.length);
		newMsg.writeBytes(content);
		ctx.channel().writeAndFlush(newMsg).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) {
				if (future.isSuccess()) {
					InetSocketAddress insocket = (InetSocketAddress) future.channel().localAddress();
//					System.out.println("ctxOfFrontHandler.fireUserEventTriggered(insocket);");
//					System.out.println(ctxOfFrontHandler);
					if (ctxOfFrontHandler != null) {
						ChannelInboundHandler handler = (ChannelInboundHandler) ctxOfFrontHandler.handler();
						try {
							handler.userEventTriggered(ctxOfFrontHandler, insocket);
						} catch (Exception e) {
							e.printStackTrace();
						}
//						ctxOfFrontHandler.fireUserEventTriggered(insocket);
					}
					ctx.fireChannelActive();
				}
			}
		});
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
//		System.out.println("-- BackendAuthHandler channelRead--");
		ByteBuf buf = (ByteBuf) msg;
		try {
			int begin = header.length() > 4 ? header.length() - 4 : 0;
			header.append(CommonMethods.ByteBufToString(buf));
			int index = header.indexOf("\r\n\r\n", begin);
			if (index > -1) {
				String headerStr = header.substring(0, index);
//				System.out.println(headerStr);
				String body = header.substring(index + 4);
				if (checkHeader(headerStr)) {
//					System.out.println("-- BackendAuthHandler channelRead checkHeader valid --");
					ctx.pipeline().remove(this);
					byte content[] = body.getBytes();
					if (content.length > 0) {
						ByteBuf newMsg = ctx.alloc().buffer(content.length);
						newMsg.writeBytes(content);
						ctx.fireChannelRead(newMsg);
					} else
						ctx.read(); // 指示BackEnd Channel 读取内容(此时传至pip Handler)
				} else {
//					System.out.println("channelRead line:102 fireChannelInactive");
					ctx.fireChannelInactive();
				}
			} else {
				System.out.println("===========not a valid head==============");
				if (header.length() > 1024 * 16) {
					ctx.fireChannelInactive();
				}
			}
		} finally {
			buf.release();
		}

	}

	final static Pattern cookiePattern = Pattern.compile("Set-Cookie: ([^=]+)=([^:]+);", Pattern.CASE_INSENSITIVE);

	public boolean checkHeader(String header) {
		if (!header.contains("auth: ok\r\n")) {
			return false;
		}
		Matcher match = cookiePattern.matcher(header);
		while (match.find()) {
			String name = match.group(1);
			String value = match.group(1);
//			System.out.println(name + ": " + value);
			LocalConfig.cookies.put(name, value);
		}
		return true;
	}

	public static String genCookie(HashMap<String, String> cookies) {
//		cookieList = [f'{key}={value}' for (key, value) in cookies.items()]
//			    return '; '.join(cookieList)
		StringJoiner sj = new StringJoiner("; ");
		for (Entry<String, String> entry : cookies.entrySet()) {
			sj.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
		}
		return sj.toString();
	}
}