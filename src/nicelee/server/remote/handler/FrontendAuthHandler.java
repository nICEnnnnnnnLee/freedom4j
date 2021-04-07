package nicelee.server.remote.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import nicelee.server.remote.RemoteConfig;
import nicelee.util.CommonMethods;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrontendAuthHandler extends ChannelInboundHandlerAdapter {

	final static Pattern patternDomain = Pattern.compile("my_domain=([^;]*)");
	final static Pattern patternPort = Pattern.compile("my_port=([0-9]+)");
	final static Pattern patternToken = Pattern.compile("my_token=([^;]*)");
	final static Pattern patternUsername = Pattern.compile("my_username=([^;]*)");
	final static Pattern patternType = Pattern.compile("my_type=([^;]*)");
	final static Pattern patternTime = Pattern.compile("my_time=([0-9]+)");

	StringBuffer header;

	public FrontendAuthHandler() {
		header = new StringBuffer();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.read(); // 指示FontEnd Channel 的FrontendAuthHandler
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
//		System.out.println("-- Remote FrontendAuthHandler channelRead--");
		ByteBuf buf = (ByteBuf) msg;
		int begin = header.length() > 4 ? header.length() - 4 : 0;
		header.append(CommonMethods.ByteBufToString(buf));
		buf.release();
		int index = header.indexOf("\r\n\r\n", begin);
		if (index > -1) {
			String headerStr = header.substring(0, index);
//			System.out.println(headerStr);
			String body = header.substring(index + 4);
			String cookieStr = getCookieStr(headerStr);
			if (cookieStr != null) {
				String domain = getValueOf(cookieStr, patternDomain);
				String port = getValueOf(cookieStr, patternPort);
				String type = getValueOf(cookieStr, patternType);
				String username = getValueOf(cookieStr, patternUsername);
				String token = getValueOf(cookieStr, patternToken);
				String timestamp = getValueOf(cookieStr, patternTime);

				if (isValid(type, domain, port, username, token, Long.parseLong(timestamp))) {
					// 回复OK 信息
					StringBuffer sb = new StringBuffer();
					sb.append("HTTP/1.1 101 Switching Protocols\r\n").append("auth: ok\r\n")
							.append("Sec-WebSocket-Accept: ").append(CommonMethods.getRandomString(24)).append("\r\n")
							.append("Upgrade: websocket\r\n").append("Connection: Upgrade\r\n\r\n");
//					System.out.println(sb.toString());
					write(ctx, sb.toString());
					// 建立单纯的连接
					final Channel inboundChannel = ctx.channel();
					Bootstrap b = new Bootstrap();
					b.group(inboundChannel.eventLoop()).channel(ctx.channel().getClass())
							.handler(new BackendInitializer(inboundChannel)).option(ChannelOption.AUTO_READ, false);
					ChannelFuture f = b.connect(domain, Integer.parseInt(port));
					f.addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) {
							if (future.isSuccess()) {
								ctx.fireUserEventTriggered(future.channel());
							} else {
								inboundChannel.close();
							}
						}
					});
					// 删除auth handler
					ctx.pipeline().remove(this);
					// 将\r\n\r\n后面的内容传给下一步handler，并将原来的buf释放
					byte content[] = body.getBytes();
					if (content.length > 0)
						ctx.fireUserEventTriggered(content);
					return;
				}
			}
			// 能走到这里，肯定是非法信息
			// 回复403 信息',
			StringBuffer sb = new StringBuffer();
			sb.append("HTTP/1.1 403 Forbidden\r\n").append("Content-Length: 0\r\n")
					.append("Connection: closed\r\n\r\n");
			write(ctx, sb.toString()).addListener(ChannelFutureListener.CLOSE);
		} else {
			if (header.length() > 1024 * 16) {
				ctx.fireChannelInactive();
			}
		}
	}

	final static Pattern cookiePattern = Pattern.compile("Cookie:(.*)", Pattern.CASE_INSENSITIVE);

	public static String getCookieStr(String header) {
		Matcher match = cookiePattern.matcher(header);
		if (match.find()) {
			return match.group(1);
		}
		return null;
	}

	static String getValueOf(String cookieStr, Pattern pattern) {
		Matcher matcher = pattern.matcher(cookieStr);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return "";
		}
	}

	static ChannelFuture write(final ChannelHandlerContext ctx, String msg) {
		byte content[] = msg.getBytes();
		ByteBuf newMsg = ctx.alloc().buffer(content.length);
		newMsg.writeBytes(content);
		return ctx.channel().writeAndFlush(newMsg);
	}

	static boolean isValid(String type, String host, String port, String username, String token, long timestamp) {
		if (System.currentTimeMillis() - timestamp > 600000) {
			// 超过10 min无效
			return false;
		}
		if ("1".equals(type) && !host.isEmpty() && !port.isEmpty() && !username.isEmpty() && !token.isEmpty()) {
			String pwd = RemoteConfig.users.get(username);
			if (pwd != null) {
				StringBuilder sb = new StringBuilder(pwd).append(RemoteConfig.salt).append(timestamp);
				String expectedToken = CommonMethods.MD5(sb.toString());
				return token.equals(expectedToken);
			}
		}
		return false;
	}
}