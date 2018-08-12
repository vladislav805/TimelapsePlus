package ru.vlad805.timelapse.handler;

import android.graphics.Bitmap;
import android.util.SparseArray;
import android.util.SparseLongArray;
import ru.vlad805.timelapse.PreferenceBundle;

/**
 * vlad805 (c) 2018
 */
public class RotateImageHandler extends ImageHandler {

	private SparseArray<Item> mData;

	public RotateImageHandler(PreferenceBundle bundle) {
		super(bundle);
	}

	private class Item {
		private double angle;

		Item(double agl) {
			angle = agl;
		}

		public double getAngle() {
			return angle;
		}
	}

	@Override
	public void onCaptureStart() {
		mData = new SparseArray<>();
	}

	@Override
	public void onImageCaptured(int index) {
		mData.put(index, null);
	}

	@Override
	public void onCaptureStop() {

	}

	@Override
	public Bitmap processImage(Bitmap image, int index) {
		return image;
	}
}
