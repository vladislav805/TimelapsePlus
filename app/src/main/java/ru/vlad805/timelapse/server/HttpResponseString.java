package ru.vlad805.timelapse.server;

import org.net.http.HttpRequestParser;

public class HttpResponseString extends HttpResponse {

	private StringBuilder mString;

	public HttpResponseString(HttpRequestParser data) {
		super(data);
		mString = new StringBuilder();
	}

	public HttpResponseString write(String str) {
		mString.append(str);
		return this;
	}

	public HttpResponseString write(CharSequence str) {
		mString.append(str);
		return this;
	}

	public HttpResponseString write(char str) {
		mString.append(str);
		return this;
	}

	@Override
	public byte[] getBytes() {
		return mString.toString().getBytes();
	}

}