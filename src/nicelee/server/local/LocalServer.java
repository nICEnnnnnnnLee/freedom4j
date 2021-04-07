package nicelee.server.local;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import nicelee.server.local.handler.FrontendInitializer;

public final class LocalServer extends Thread {

	public static void main(String[] args) throws Exception {
		LocalConfig.init(null);
		new LocalServer().run();
	}

	@Override
	public void run() {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new FrontendInitializer()).childOption(ChannelOption.AUTO_READ, false)
					.bind(LocalConfig.localPort).sync().channel().closeFuture().sync();
			ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

	}

}