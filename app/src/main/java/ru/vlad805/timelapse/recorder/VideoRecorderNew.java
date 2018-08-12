package ru.vlad805.timelapse.recorder;

import android.graphics.Bitmap;
import ru.vlad805.timelapse.MotionJpegGenerator;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class VideoRecorderNew {

	private MotionJpegGenerator mVideo = null;
	private int mFrameCount = 0;
	private final int mQuality;

	public VideoRecorderNew(String path, String filename, int width, int height, double frameRate, int quality) {
		mQuality = quality;
		try {
			mVideo = new MotionJpegGenerator(new File(path + File.separator + filename), width, height, frameRate, 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addFrame(Bitmap data) {
		mFrameCount++;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			data.compress(Bitmap.CompressFormat.JPEG, mQuality, stream);
			mVideo.addFrame(stream.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			data.recycle();
		}
	}

	public void save() {
		try {
			mVideo.finishAVI();
			mVideo.fixAVI(mFrameCount);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public long getFileSize() {
		return mVideo.getFile().length();
	}
}
