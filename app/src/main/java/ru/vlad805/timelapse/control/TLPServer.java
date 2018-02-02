package ru.vlad805.timelapse.control;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.net.http.HttpRequestParser;
import ru.vlad805.timelapse.CameraAdapter;
import ru.vlad805.timelapse.Setting;
import ru.vlad805.timelapse.SettingsBundle;
import ru.vlad805.timelapse.activity.TimeLapseActivity;
import ru.vlad805.timelapse.server.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * vlad805 (c) 2018
 */
public class TLPServer extends Server implements IControl, Server.OnRequestListener {

	private CameraAdapter mCamera;
	private SettingsBundle mSettings;
	private Context mContext;
	private WeakReference<byte[]> mLastCapture = new WeakReference<>(null);

	public TLPServer(CameraAdapter camera, SettingsBundle settings, Context context) {
		super(7394);
		mCamera = camera;
		mSettings = settings;
		mContext = context;
		Log.e("SRV", "TLPServer: create");
		setRequestListener(this);
	}

	public void setLastCapture(byte[] data) {
		mLastCapture = new WeakReference<>(data);
	}

	@Override
	public HttpResponse onRequest(HttpRequestParser request) {
		HttpResponse res = new HttpResponseString(request);

		switch (request.getPath()) {
			case "/":
				try (Reader isr = new InputStreamReader(mContext.getAssets().open("remote/index.html"))) {
					int tmp;
					while ((tmp = isr.read()) != -1) {
						((HttpResponseString) res).write((char) tmp);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;

			case "/getImage":
				Log.i("Impl", "SENDING");
				if (mLastCapture.get() != null) {
					res = new HttpResponseBinary(request);
					((HttpResponseBinary) res.setMimeType("image/jpeg")).write(mLastCapture.get());
				} else {
					res.setHttpCode(HttpCode.CODE_404_NOT_FOUND);
				}
				break;

			case "/getSettings":
				res = new HttpResponseJSON(request);
				((HttpResponseJSON) res).write(getJSONSettings());
				break;

			case "/setSetting":
				res = new HttpResponseJSON(request);

				Log.i("TLPS", "onRequest: name=" + request.getQueryParam("name") + "; value=" + request.getQueryParam("value"));

				setSettingItem(request.getQueryParam("name"), request.getQueryParam("value"));

				((HttpResponseJSON) res).write("{\"result\":true}");
				break;

			case "/control/autoFocus":
				res = new HttpResponseJSON(request);
				mCamera.autoFocus(null);
				((HttpResponseJSON) res).write("{\"result\":true}");
				break;

			default:
				((HttpResponseString) res).write("404");
		}

		return res;
	}

	private void setSettingItem(String name, String value) {
		switch (name) {
			case Setting.EFFECT:
				mCamera.setEffect(value);
				mSettings.setEffect(value);
				mCamera.setup();
				break;

			case Setting.WHITE_BALANCE:
				mCamera.setWhiteBalance(value);
				mSettings.setBalance(value);
				mCamera.setup();
				break;

			case Setting.FLASH_MODE:
				mCamera.setFlashMode(value);
				mSettings.setFlashMode(value);
				mCamera.setup();
				break;

			case Setting.QUALITY:
				mCamera.setQuality(Integer.valueOf(value));
				mSettings.setQuality(Integer.valueOf(value));
				mCamera.setup();
				break;

			case Setting.FPS:
				mSettings.setFPS(Integer.valueOf(value));
				//((TimeLapseActivity) mContext).updateSettingsPreview();
				break;

			case Setting.DELAY:
				mSettings.setDelay(Integer.valueOf(value));
				//((TimeLapseActivity) mContext).updateSettingsPreview();
				break;

			case Setting.INTERVAL:
				mSettings.setInterval(Integer.valueOf(value));
				//((TimeLapseActivity) mContext).updateSettingsPreview();
				break;
		}
	}

	private class HttpResponseJSON extends HttpResponseString {

		public HttpResponseJSON(HttpRequestParser data) {
			super(data);
			setMimeType("application/json");
		}

	}

	private String getJSONSettings() {
		try {
			JSONObject wrap = new JSONObject();
			HashMap<String, Object> current = new HashMap<>();

			current.put(Setting.EFFECT, mSettings.getEffect());
			current.put(Setting.DELAY, mSettings.getDelay());
			current.put(Setting.INTERVAL, mSettings.getInterval());
			current.put(Setting.QUALITY, mSettings.getQuality());
			current.put(Setting.WHITE_BALANCE, mSettings.getBalance());
			current.put(Setting.HANDLER, mSettings.getImageHandler());
			current.put(Setting.FLASH_MODE, mSettings.getFlashMode());
			current.put(Setting.FPS, mSettings.getFPS());
			current.put(Setting.RECORD_MODE, mSettings.getRecordMode());
			current.put(Setting.WORK_DIRECTORY, mSettings.getPath());
			current.put(Setting.SIZE, mSettings.getWidth() + "x" + mSettings.getHeight());

			HashMap<String, Object> available = new HashMap<>();

			available.put(Setting.EFFECT, getJSONArrayFromArray(mCamera.getAvailableEffects()));
			available.put(Setting.WHITE_BALANCE, getJSONArrayFromArray(mCamera.getAvailableWhiteBalance()));
			available.put(Setting.FLASH_MODE, getJSONArrayFromArray(mCamera.getAvailableFlashMode()));
			available.put(Setting.SIZE, getJSONArrayFromArray(mCamera.getAvailablePictureSize()));
			available.put(Setting.RECORD_MODE, getJSONArrayFromArray(new Integer[] {
					Setting.RecordMode.VIDEO,
					Setting.RecordMode.PHOTO_DIR
			}));
			available.put(Setting.HANDLER, getJSONArrayFromArray(new Integer[] {
					Setting.ImageHandler.NONE,
					Setting.ImageHandler.INSERT_DATE_AND_TIME
			}));

			wrap.put("current", getJSONFromMap(current));
			wrap.put("supported", getJSONFromMap(available));

			return wrap.toString();
		} catch (JSONException e) {
			return null;
		}
	}

	private JSONObject getJSONFromMap(Map<String, Object> data) throws JSONException {
		JSONObject res = new JSONObject();
		for (String key : data.keySet()) {
			res.put(key, data.get(key));
		}
		return res;
	}

	public JSONArray getJSONArrayFromArray(Object[] data) {
		JSONArray res = new JSONArray();
		if (data != null) {
			for (Object item : data) {
				res.put(item);
			}
		}
		return res;
	}
}
