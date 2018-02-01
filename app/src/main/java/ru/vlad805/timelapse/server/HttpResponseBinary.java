package ru.vlad805.timelapse.server;

import org.net.http.HttpRequestParser;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public class HttpResponseBinary extends HttpResponse {

	private List<Byte> mBinary;

	public HttpResponseBinary(HttpRequestParser data) {
		super(data);
		mBinary = new ArrayList<>();
	}

	public HttpResponseBinary write(byte[] data) {
		for (byte i : data) {
			write(i);
		}
		return this;
	}

	public HttpResponseBinary write(byte data) {
		mBinary.add(data);
		return this;
	}

	@Override
	public byte[] getBytes() {
		byte d[] = new byte[mBinary.size()];

		int i = 0;
		for (byte b : mBinary) {
			d[i++] = b;
		}

		return d;
	}

}