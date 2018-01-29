package ru.vlad805.timelapse.server;

import android.util.Log;
import org.net.http.HttpRequestParser;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Server {

	private HttpServerThread mServerThread;
	private ServerSocket httpServerSocket;

	@SuppressWarnings("UnnecessaryInterfaceModifier")
	public interface OnRequestListener {
		public HttpResponse onRequest(HttpRequestParser request) throws IOException;
	}

	public Server(int port) {
		log("Server creating...");
		mServerThread = new HttpServerThread(port);
		log("Server created");
	}

	private void log(String s) {
		Log.i("TLServer", s);
	}

	public void start() {
		log("Server starting...");
		mServerThread.start();
		log("Server started");
	}

	public void stop() {
		log("Server stopping...");
		if (httpServerSocket != null) {
			try {
				httpServerSocket.close();
				log("Server stoped");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setRequestListener(OnRequestListener listener) {
		mServerThread.setRequestListener(listener);
	}

	private String getIpAddress() {
		StringBuilder ip = new StringBuilder();
		try {
			Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (enumNetworkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
				Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
				while (enumInetAddress.hasMoreElements()) {
					InetAddress inetAddress = enumInetAddress.nextElement();
					if (inetAddress.isSiteLocalAddress()) {
						ip.append("SiteLocalAddress: ").append(inetAddress.getHostAddress()).append("\n");
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
			ip.append("Something Wrong! ").append(e.toString()).append("\n");
		}

		return ip.toString();
	}

	private class HttpServerThread extends Thread {

		private OnRequestListener mRequestListener;
		private int mPort;

		public HttpServerThread(int port) {
			log("Create server thread for " + port + " port");
			mPort = port;
		}

		public void setRequestListener(OnRequestListener listener) {
			log("Set request listener for server thread");
			mRequestListener = listener;
		}

		@Override
		public void run() {
			try {
				httpServerSocket = new ServerSocket(mPort);
				log("ServerSocket started");

				//noinspection InfiniteLoopStatement
				while (true) {
					Socket socket = httpServerSocket.accept();
					log("Server socket accepted");

					HttpRequestParser req = new HttpRequestParser();
					log("Request parsed");

					req.parseRequest(new BufferedReader(new InputStreamReader(socket.getInputStream())));

					log("Prepare for response");
					if (mRequestListener != null) {
						log("Sending response...");
						new HttpResponseThread(socket, mRequestListener.onRequest(req)).start();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
