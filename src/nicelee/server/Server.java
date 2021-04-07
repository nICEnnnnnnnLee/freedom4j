package nicelee.server;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
import nicelee.server.local.LocalConfig;
import nicelee.server.local.LocalServer;
import nicelee.server.remote.RemoteConfig;
import nicelee.server.remote.RemoteServer;
import nicelee.util.ConfigUtil;

public final class Server {

	public static void main(String[] args) throws Exception {
//		InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
//        Logger.getLogger("io.netty").setLevel(Level.OFF);
		String configPath = (args != null && args.length > 0) ? args[0] : "app.config";
		
		HashMap<String, String> settings = ConfigUtil.initConfigs(configPath);
		String configType = settings.getOrDefault("configType", "local");
		if ("local".equals(configType)) {
			LocalConfig.init(settings);
			new LocalServer().start();
		} else if ("remote".equals(configType)) {
			RemoteConfig.init(settings);
			new RemoteServer().start();
		}
	}

}