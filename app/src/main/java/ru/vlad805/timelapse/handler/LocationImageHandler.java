package ru.vlad805.timelapse.handler;

import android.graphics.Bitmap;
import android.util.SparseArray;
import ru.vlad805.timelapse.PreferenceBundle;

/**
 * vlad805 (c) 2018
 */
public class LocationImageHandler extends ImageHandler {

	private SparseArray<Item> mData;

	public LocationImageHandler(PreferenceBundle bundle) {
		super(bundle);
	}

	private class Item {
		private double latitude;
		private double longitude;
		private float speed;
		private int bearing;

		Item(double a, double o, float s, int b) {
			latitude = a;
			longitude = o;
			speed = s;
			bearing = b;
		}

		public double getLatitude() {
			return latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public float getSpeed() {
			return speed;
		}

		public int getBearing() {
			return bearing;
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
