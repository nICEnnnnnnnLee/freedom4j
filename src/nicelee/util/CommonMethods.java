package nicelee.util;

import java.security.MessageDigest;
import java.util.Random;

import io.netty.buffer.ByteBuf;

public class CommonMethods {

	public static String MD5(String dataStr) {
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.update(dataStr.getBytes("UTF8"));
			byte s[] = m.digest();
			StringBuffer result = new StringBuffer();
			for (int i = 0; i < s.length; i++) {
				result.append(Integer.toHexString((0x000000FF & s[i]) | 0xFFFFFF00).substring(6));
			}
			return result.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	public static String ByteBufToString(ByteBuf buf) {
		String str;
		if (buf.hasArray()) { // 处理堆缓冲区
			str = new String(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes());
		} else { // 处理直接缓冲区以及复合缓冲区
			byte[] bytes = new byte[buf.readableBytes()];
			buf.getBytes(buf.readerIndex(), bytes);
			str = new String(bytes, 0, buf.readableBytes());
		}
		return str;
	}

	final public static String dictionary = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	public static String getRandomString(int length) {
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int number = random.nextInt(62);
			sb.append(dictionary.charAt(number));
		}
		return sb.toString();
	}

	public static String ipIntToString(int ip) {
		return String.format("%s.%s.%s.%s", (ip >> 24) & 0x00FF, (ip >> 16) & 0x00FF, (ip >> 8) & 0x00FF, ip & 0x00FF);
	}

	public static String ipBytesToString(byte[] ip) {
		return String.format("%s.%s.%s.%s", ip[0] & 0x00FF, ip[1] & 0x00FF, ip[2] & 0x00FF, ip[3] & 0x00FF);
	}

	public static short readShort(byte[] data, int offset) {
		int r = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
		return (short) r;
	}
}
