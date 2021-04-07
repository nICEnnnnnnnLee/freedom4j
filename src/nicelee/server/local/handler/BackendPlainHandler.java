package nicelee.server.local.handler;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 配合FrontendSocks5Handler 通过userEvent 传递一个InetSocketAddress地址
 * 
 */
public class BackendPlainHandler extends ChannelInboundHandlerAdapter {

	final ChannelHandlerContext ctxOfFrontHandler;

	public BackendPlainHandler(ChannelHandlerContext ctx) {
		this.ctxOfFrontHandler = ctx;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		InetSocketAddress insocket = (InetSocketAddress) ctx.channel().localAddress();
		if (ctxOfFrontHandler != null) {
			ChannelInboundHandler handler = (ChannelInboundHandler) ctxOfFrontHandler.handler();
			try {
				handler.userEventTriggered(ctxOfFrontHandler, insocket);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ctx.pipeline().remove(this);
		ctx.fireChannelActive();
	}
}