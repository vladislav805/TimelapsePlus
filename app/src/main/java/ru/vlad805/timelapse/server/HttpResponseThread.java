package ru.vlad805.timelapse.server;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class HttpResponseThread extends Thread {

	private Socket mSocket;
	private HttpResponse mResponse;

	public HttpResponseThread(Socket socket, HttpResponse response) {
		mSocket = socket;
		mResponse = response;
	}

	@Override
	public void run() {
		byte[] result = mResponse.getBytes();
		byte header[] = ("HTTP/1.1 " + mResponse.getHttpCode() + "\r\nContent-type: " + mResponse.getMimeType() + "\r\nContent-length: " + result.length + "\r\n\r\n").getBytes();

		try (OutputStream os = mSocket.getOutputStream()) {
			os.write(header);
			os.write(result);

			os.flush();
			os.close();
			Log.i("TLServer", "Request from " + mSocket.getInetAddress().toString() + "\n");
			mSocket.close();
			Log.i("TLServer", "Socket closed\n");
		} catch (IOException e) {
			if (mSocket != null) {
				try {
					mSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		}
	}
}