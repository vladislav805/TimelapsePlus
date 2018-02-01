package ru.vlad805.timelapse.server;

import org.net.http.HttpRequestParser;

abstract public class HttpResponse {

	private HttpRequestParser mData;
	private String mCode = HttpCode.CODE_200_OK;
	private String mMime;

	protected HttpResponse(HttpRequestParser data) {
		mData = data;
	}

	public HttpResponse setMimeType(String type) {
		mMime = type;
		return this;
	}

	public String getMimeType() {
		return mMime;
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