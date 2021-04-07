package nicelee.server.remote;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map.Entry;

public class RemoteConfig {

	public static int port = 8080;
	public static String salt = "4567";
	public static boolean useSSL = false;
	public static String sslCertPath;
	public static String sslKeyPath;
	public static String fqdn = "localhost"; // 使用SSL且没有配置证书路径时生效，自签证书的fqdn a fully qualified domain name
	final public static HashMap<String, String> users = new HashMap<>();
	static {
		users.put("admin", "admin");
		users.put("username", "pwd");
	}

	public static void init(HashMap<String, String> settings) {

		for (Field field : RemoteConfig.class.getDeclaredFields()) {
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

		users.clear();
		for (Entry<String, String> entry : settings.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (key.startsWith("users.")) {
				users.put(key.substring(6), value);
			}
		}
	}
}