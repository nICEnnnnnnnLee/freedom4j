package nicelee.server.local;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;

import nicelee.util.TrustAllSSLUitl;

public class LocalConfig {

	public static int localPort = 80;
	public static String remoteHost = "127.0.0.1";
	public static int remotePort = 8080;
	public static String salt = "4567";
	public static String username = "username";
	public static String password = "pwd";

	public static boolean dircectIfCN = false;
	public static boolean useSSLRemote = false;
	public static boolean verifySSLRemote = false;
	public static SSLContext sslContext = null;
	// 用于HTTP头部
	public static String path = "/";
	public static String http_version = "1.1";
	public static String domain = "127.0.0.1";
	public static String port = "5000";
	public static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0";

	final public static ConcurrentHashMap<String, String> cookies = new ConcurrentHashMap<>();

	public static void init(HashMap<String, String> settings) {
		if (settings != null && settings.size() > 0) {
			for (Field field : LocalConfig.class.getDeclaredFields()) {
				String name = field.getName();
				String value = settings.get(name);
				try {
					if (value != null) {
						if (field.getType().equals(int.class)) {
							field.set(null, Integer.parseInt(value));
						} else if (field.getType().equals(boolean.class)) {
							field.set(null, "true".equalsIgnoreCase(value));
						} else if (field.getType().equals(String.class)) {
							field.set(null, value);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		cookies.put("my_type", "1");
		cookies.put("my_username", username);
//		cookies.put("my_domain", "127.0.0.1");
//		cookies.put("my_port", "5000");
//		cookies.put("my_token", CommonMethods.MD5(password + salt));
//		cookies.put("my_time", System.currentTimeMillis());
		if (useSSLRemote) {
			sslContext = TrustAllSSLUitl.getSSLContext(verifySSLRemote);
		}
	}

}