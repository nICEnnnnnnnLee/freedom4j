package nicelee.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigUtil {

	final static Pattern patternConfig = Pattern.compile("^[ ]*([0-9|a-z|A-Z|.|_]+)[ ]*=[ ]*([^ ]+.*$)");

	public static HashMap<String, String> initConfigs(String configPath) {
		HashMap<String, String> settings = new HashMap<>();
		System.out.println("----Config init begin...----");
		readConfig(configPath, settings);
		System.out.println("----Config ini end...----");
		return settings;
	}

	private static void readConfig(String path, HashMap<String, String> settings) {
		File configFile = ResourcesUtil.search(path);
		if (configFile != null) {
			try (BufferedReader buReader = new BufferedReader(new FileReader(configFile))) {
				String config = buReader.readLine();
				while (config != null) {
					Matcher matcher = patternConfig.matcher(config);
					if (matcher.find()) {
						settings.put(matcher.group(1), matcher.group(2).trim());
						System.out.printf("  key-->value:  %s --> %s\r\n", matcher.group(1), matcher.group(2));
					}
					config = buReader.readLine();
				}
			} catch (IOException e) {
				System.out.println("配置文件不存在! ");
			}
		}
	}
}
