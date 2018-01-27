package ru.vlad805.timelapse;

import java.io.File;

public class VideoRecorder {
	private int mFrameCount = 0;
	private MotionJpegGenerator mVideo = null;

	public VideoRecorder(String filename, int width, int height, double framerate) {
		try {
			mVideo = new MotionJpegGenerator(new File(filename), width, height, framerate, 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			mVideo.finishAVI();
			mVideo.fixAVI(mFrameCount);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addFrame(byte[] data) {
		mFrameCount++;
		try {
			mVideo.addFrame(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getFrameCount() {
		return mFrameCount;
	}

	public long getFileSize() {
		return mVideo.aviFile.length();
	}
}
