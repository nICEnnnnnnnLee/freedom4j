package nicelee.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 根据apnic公开的cn地址段，判断某个ip地址是否为国内ip地址。
 * https://gitee.com/joymufeng/cn-ip-recognizer
 * 
 * @author joymufeng
 */
public class CNIPRecognizerGenerate {

	final static class CNRecord {
		public long start = 0;
		public int count = 0;

		public CNRecord(long start, int count) {
			this.start = start;
			this.count = count;
		}

		public boolean contains(long ipValue) {
			return ipValue >= start && ipValue <= start + count;
		}
	}

	public static long ipToLong(String ipAddress) {
		String[] addrArray = ipAddress.split("\\.");

		long num = 0;
		for (int i = 0; i < addrArray.length; i++) {
			int power = 3 - i;
			num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
		}
		return num;
	}

	public static void test() {
		String ipAddrs[] = { "8.8.8.8", // Google DNS
				"116.207.137.19", // www.bilibili.com
				"14.215.177.38", // www.baidu.com
		};
		for (String ipAddr : ipAddrs) {
			boolean isCNIp = CNIPRecognizer.isCNIP(ipAddr);
			System.out.printf("%s 是CN IP: %s\n", ipAddr, isCNIp);
		}
	}

	public static void main(String args[]) {
		String src = "C:\\Downloads\\delegated-apnic-latest";
		String dest = "D:\\Workspace\\GitWorkspace\\CN_IP.java";
		generate(src, dest);
		// test();
	}

	public static void generate(String src, String dest) {
		try (BufferedReader reader = new BufferedReader(new FileReader(src));
				BufferedWriter writer = new BufferedWriter(new FileWriter(dest))) {
			long timeStart = System.currentTimeMillis();
			List<CNRecord> recordsRaw = new ArrayList<>(9000);
			// 读取合法记录
			String line = reader.readLine();
			while (line != null) {
				if (line.startsWith("apnic|CN|ipv4|")) {
					String[] params = line.split("\\|");
					if (params.length == 7) {
						long ipAddr = ipToLong(params[3]);
						int ipLen = Integer.parseInt(params[4]);
						recordsRaw.add(new CNRecord(ipAddr, ipLen));
					}
				}
				line = reader.readLine();
			}
			// 按照ip大小排序
			Collections.sort(recordsRaw, new Comparator<CNRecord>() {
				@Override
				public int compare(CNRecord o1, CNRecord o2) {
					if (o1.start < o2.start) {
						return -1;
					} else if (o1.start > o2.start) {
						return 1;
					} else {
						return 0;
					}
				}
			});

			// 合并记录
			List<CNRecord> recordsMerge = new ArrayList<>(8000);
			System.out.println("合并前记录长度:" + recordsRaw.size());
			for (CNRecord record : recordsRaw) {
				CNRecord lastRecord = recordsMerge.size() == 0 ? new CNRecord(0, 0)
						: recordsMerge.get(recordsMerge.size() - 1);
				if (lastRecord.start + lastRecord.count == record.start) {
					lastRecord.count = lastRecord.count + record.count;
				} else {
					recordsMerge.add(record);
				}
			}
			System.out.println("合并后记录长度:" + recordsMerge.size());

			// 输出格式化结果
			for (int i = 0; i < recordsMerge.size(); i++) {
				if (i % 1000 == 0) {
					if (i > 0) {
						writer.write("}\n");
					}
					writer.write(String.format("private static void init%d(List<CNRecord> list) {\n", i / 1000));
				}
				CNRecord record = recordsMerge.get(i);
				writer.write(String.format("list.add(new CNRecord(%dL, %d));\n", record.start, record.count));
			}
			writer.write("}\n");
			long timeStop = System.currentTimeMillis();
			System.out.println("耗时(秒): " + (timeStop - timeStart) / 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
