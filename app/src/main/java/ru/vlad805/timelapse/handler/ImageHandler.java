package ru.vlad805.timelapse.handler;

import android.graphics.Bitmap;
import ru.vlad805.timelapse.PreferenceBundle;

/**
 * vlad805 (c) 2018
 */
public abstract class ImageHandler {

	private PreferenceBundle mBundle;

	ImageHandler(PreferenceBundle bundle) {
		mBundle = bundle;
	}

	protected PreferenceBundle getPreference() {
		return mBundle;
	}

	public abstract void onCaptureStart();

	public abstract void onImageCaptured(int index);

	public abstract void onCaptureStop();

	public abstract Bitmap processImage(Bitmap image, int index);

}
