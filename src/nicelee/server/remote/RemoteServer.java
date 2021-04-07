package nicelee.server.remote;

import java.io.File;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.ResourceLeakDetector;
import nicelee.server.remote.handler.FrontendInitializer;

public final class RemoteServer extends Thread {

	public static void main(String[] args) throws Exception {

		new RemoteServer().run();
	}

	@Override
	public void run() {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			SslContext sslContext = getSSLContext();
			System.out.printf("正在监听端口: %s, SSL服务已开启: %s\n", RemoteConfig.port, sslContext != null);
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new FrontendInitializer(sslContext)).childOption(ChannelOption.AUTO_READ, false)
					.bind(RemoteConfig.port).sync().channel().closeFuture().sync();
			ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

	}

	public static SslContext getSSLContext() {
		try {
			if (RemoteConfig.useSSL) {
				if (RemoteConfig.sslCertPath != null && RemoteConfig.sslKeyPath != null) {
					return getSslContext(new File(RemoteConfig.sslCertPath), new File(RemoteConfig.sslKeyPath), null);
				} else {
					SelfSignedCertificate ssc = new SelfSignedCertificate(RemoteConfig.fqdn, new SecureRandom(), 2048);
					SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
					return sslCtx;
				}
			}
		} catch (SSLException | CertificateException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static SslContext getSslContext(File certChainFile, File keyFile, File rootFile) throws SSLException {
		SslContext sslCtx = SslContextBuilder.forServer(certChainFile, keyFile)
				// .trustManager(rootFile)
				// .protocols("TLSv1.2")
				.clientAuth(ClientAuth.NONE).build();
		return sslCtx;
	}

}