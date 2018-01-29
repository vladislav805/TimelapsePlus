package ru.vlad805.timelapse.server;

import org.net.http.HttpRequestParser;

abstract public class HttpResponse {

	private HttpRequestParser mData;
	private String mCode = HttpCode.CODE_200_OK;

	protected HttpResponse(HttpRequestParser data) {
		mData = data;
	}

	public String getMimeType() {
		return "text/html";
	}

	protected final HttpRequestParser getRequest() {
		return mData;
	}

	public abstract byte[] getBytes();

	public final HttpResponse setHttpCode(String code) {
		mCode = code;
		return this;
	}

	public final String getHttpCode() {
		return mCode;
	}
}