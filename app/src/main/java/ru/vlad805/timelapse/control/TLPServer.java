package ru.vlad805.timelapse.control;

import org.net.http.HttpRequestParser;
import ru.vlad805.timelapse.SettingsBundle;
import ru.vlad805.timelapse.server.*;

import java.io.IOException;

/**
 * vlad805 (c) 2018
 */
public class TLPServer extends Server implements IControl, Server.OnRequestListener {

	private SettingsBundle mSettings;

	public TLPServer(SettingsBundle settings) {
		super(7394);
		mSettings = settings;
	}


	@Override
	public HttpResponse onRequest(HttpRequestParser request) throws IOException {
		HttpResponse res = new HttpResponseString(request);

		switch (request.getPath()) {
			case "/":
				((HttpResponseString) res).write("main");
				break;

			case "/getImage":
				/*if (mLastCapture != null) {
					res = new HttpResponseBinary(request);
					((HttpResponseBinary) res).write(mLastCapture).setMimeType("image/jpeg");
				} else {
					res.setHttpCode(HttpCode.CODE_404_NOT_FOUND);
				}*/

				break;

			default:
				((HttpResponseString) res).write("404");
		}

		return res;
	}
}
