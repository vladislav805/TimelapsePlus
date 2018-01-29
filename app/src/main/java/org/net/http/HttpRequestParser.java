package org.net.http;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Hashtable;

/**
 * Class for HTTP request parsing as defined by RFC 2612:
 *
 * Request = Request-Line ; Section 5.1 (( general-header ; Section 4.5 |
 * request-header ; Section 5.3 | entity-header ) CRLF) ; Section 7.1 CRLF [
 * message-body ] ; Section 4.3
 *
 * @author izelaya
 *
 */
public class HttpRequestParser {

	private String mRequestLine;
	private Hashtable<String, String> mRequestHeaders;
	private StringBuffer mMessageBody;
	
	public enum Method {
		GET, POST, PUT, DELETE, UNKNOWN
	}
	
	private Method mMethod;
	private String mPath;
	private Hashtable<String, String> mQueryParams;

	public HttpRequestParser() {
		mRequestHeaders = new Hashtable<>();
		mMessageBody = new StringBuffer();
		mQueryParams = new Hashtable<>();
	}

	/**
	 * Parse and HTTP request.
	 */
	public void parseRequest(BufferedReader reader) throws IOException {
		setRequestLine(reader.readLine());

		String header;

		do {
			header = reader.readLine();
			Log.i("PARSER", "parseRequest-head: " + header + "; length: " + header.length());

			if (!header.isEmpty()) {
				appendHeaderParameter(header);
			}
		} while ((header = reader.readLine()) != null && header.length() > 1);
		Log.i("PARSER", "done");
		/*
		String bodyLine = reader.readLine();
		Log.i("PARSER", "parseRequest-before-body: " + bodyLine);
		while (bodyLine != null && !bodyLine.isEmpty()) {
			appendMessageBody(bodyLine);
			bodyLine = reader.readLine();
			Log.i("PARSER", "parseRequest-body: " + bodyLine);
		}*/
		//reader.close();

		parseStartString();
	}

	private void parseStartString() throws UnsupportedEncodingException {
		String params[], temp[];


		String cmd[] = mRequestLine.split("\\s");
		if (cmd.length != 3) {
			throw new IllegalArgumentException();
		}

		mMethod = parseMethod(cmd[0]);
		
		int idx = cmd[1].indexOf('?');
		
		if (idx < 0) {
			mPath = cmd[1];
		} else {
			mPath = URLDecoder.decode(cmd[1].substring(0, idx), "ISO-8859-1");
			params = cmd[1].substring(idx + 1).split("&");
			
			for (String item : params) {
				temp = item.split("=");
				if (temp.length == 2) {
					mQueryParams.put(URLDecoder.decode(temp[0], "ISO-8859-1"), 	URLDecoder.decode(temp[1], "ISO-8859-1")); 
				} else if (temp.length == 1 && item.indexOf('=') == item.length() - 1) {
					mQueryParams.put(URLDecoder.decode(temp[0], "ISO-8859-1"), "");
				}
			}
		}
	}
	
	private Method parseMethod(String m) {
		switch (m) {
			case "GET": return Method.GET;
			case "POST": return Method.POST;
			case "PUT": return Method.PUT;
			case "DELETE": return Method.DELETE;
			default: return Method.UNKNOWN;
		}
	}

	public String getPath() {
		return mPath;
	}

	public Method getMethod() {
		return mMethod;
	}

	public Hashtable<String, String> getQueryParams() {
		return mQueryParams;
	}

	public String getQueryParam(String key) {
		return mQueryParams.containsKey(key) ? mQueryParams.get(key) : null;
	}

	/**
	 *
	 * 5.1 Request-Line The Request-Line begins with a method token, followed by
	 * the Request-URI and the protocol version, and ending with CRLF. The
	 * elements are separated by SP characters. No CR or LF is allowed except in
	 * the final CRLF sequence.
	 *
	 * @return String with Request-Line
	 */
	public String getRequestLine() {
		return mRequestLine;
	}

	private void setRequestLine(String requestLine)  {
		if (requestLine == null || requestLine.length() == 0) {
			throw new IllegalArgumentException("Invalid Request-Line: " + requestLine);
		}
		mRequestLine = requestLine;
	}

	private void appendHeaderParameter(String header) {
		int idx = header.indexOf(":");
		if (idx == -1) {
			throw new IllegalArgumentException("Invalid Header Parameter: " + header);
		}
		mRequestHeaders.put(header.substring(0, idx), header.substring(idx + 1, header.length()));
	}

	/**
	 * The message-body (if any) of an HTTP message is used to carry the
	 * entity-body associated with the request or response. The message-body
	 * differs from the entity-body only when a transfer-coding has been
	 * applied, as indicated by the Transfer-Encoding header field (section
	 * 14.41).
	 * @return String with message-body
	 */
	public String getMessageBody() {
		return mMessageBody.toString();
	}

	private void appendMessageBody(String bodyLine) {
		mMessageBody.append(bodyLine).append("\r\n");
	}

	/**
	 * For list of available headers refer to sections: 4.5, 5.3, 7.1 of RFC 2616
	 * @param headerName Name of header
	 * @return String with the value of the header or null if not found.
	 */
	public String getHeaderParam(String headerName){
		return mRequestHeaders.get(headerName);
	}
}