package nicelee.http.sample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class A0_PrintTCPContentClient {

	public static void main(String[] args) {
		Socket socket = null;
		try {
			System.out.println("开始连接... ");
			socket=new Socket();
			socket.bind(new InetSocketAddress(9867));
			socket.connect(new InetSocketAddress("127.0.0.1", 80));
//			socket.connect(new InetSocketAddress("m.baidu.com", 80));
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			System.out.println("发送请求... ");
			
//			writer.write("CONNECT wx3.sinaimg.cn:443 HTTP/1.1\r\n");
//			writer.write("Proxy-Connection: keep-alive\r\n");
			writer.write("GET / HTTP/1.1\r\n");
			writer.write("Host: www.baidu.com:80\r\n");
			writer.write("Connection: keep-alive\r\n");
			//writer.write("content-length: 12\r\n");
			//writer.write("range: bytes=3812919-\r\n");
			writer.write("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n");
			writer.write("User-Agent: Mozilla/5.0 (Linux; Android 8.0; DUK-AL20 Build/HUAWEIDUK-AL20; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/48.0.2564.116 Mobile Safari/537.36 T7/9.1 baidubrowser/7.18.21.0 (Baidu; P1 8.0.0)\r\n");
//			writer.write("Accept-Encoding: gzip, deflate, br\r\n");
			writer.write("Accept-Language: zh-CN,en-US;q=0.8\r\n\r\n");
//			writer.write(new char[12]);
//			writer.write("\r\n");
			writer.flush();
			
			
			System.out.println("等待回复... ");
			String data;
			while( (data = reader.readLine() ) != null) {
				System.out.println(data);
			}
			//writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
