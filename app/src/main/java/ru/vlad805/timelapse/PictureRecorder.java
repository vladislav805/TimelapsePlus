package ru.vlad805.timelapse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class PictureRecorder implements IRecorder {

	private int mTotalSize = 0;
	private int mFrameCount = 0;
	private String mPath;
	private ArrayList<String> mPhotos;
	private long mStart;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public PictureRecorder(String path, String name) {
		mPhotos = new ArrayList<>();
		mPath = path + File.separator + name;
		mStart = System.currentTimeMillis() / 1000;

		File f = new File(mPath);

		if (f.exists()) {
			throw new RuntimeException("Already exists");
		}

		f.mkdirs();
	}

	public void stop() {
		File sum = new File(mPath + File.separator + "index.tlif");
		try (FileOutputStream fos = new FileOutputStream(sum)) {
			fos.write(generateJSON().getBytes());
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	public void addFrame(byte[] data) {
		mFrameCount++;

		mPhotos.add(mFrameCount + ".jpg");

		File cur = new File(mPath + File.separator + mFrameCount + ".jpg");

		try (FileOutputStream fos = new FileOutputStream(cur)) {
			fos.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mTotalSize += cur.length();
	}

	public int getFrameCount() {
		return mFrameCount;
	}

	public long getFileSize() {
		return mTotalSize;
	}

	private String generateJSON() throws JSONException {
		return new JSONObject()
				.put("meta",
						new JSONObject()
								.put("version", Setting.TLIF_VERSION)
								.put("date", mStart)
								.put("frames", getFrameCount())
				)
				.put("items", new JSONArray(mPhotos))
				.toString();
	}

}
