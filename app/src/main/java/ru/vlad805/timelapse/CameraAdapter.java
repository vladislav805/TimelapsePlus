package ru.vlad805.timelapse;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.security.Policy;
import java.util.List;
import java.util.Locale;

/**
 * vlad805 (c) 2018
 */
@SuppressWarnings("deprecation")
public class CameraAdapter {

	private Context mContext;
	private Camera mCamera;
	private SettingsBundle mSettings;

	public class Size {
		private int mWidth;
		private int mHeight;

		private Size(Camera.Size s) {
			mWidth = s.width;
			mHeight = s.height;
		}

		public int getWidth() {
			return mWidth;
		}

		public int getHeight() {
			return mHeight;
		}

		@Override
		public String toString() {
			return String.format(Locale.ENGLISH, "%dx%d", mWidth, mHeight);
		}
	}

	private String[] mAvailableWhiteBalance;
	private Size[] mAvailablePictureSize;
	private Size[] mAvailablePreviewSize;
	private String[] mAvailableEffects;
	private String[] mAvailableFlashMode;

	public CameraAdapter(Context context, SettingsBundle settings) {
		mContext = context;
		mSettings = settings;
	}

	public void setCamera(Camera camera) {
		mCamera = camera;

		Camera.Parameters p = mCamera.getParameters();
		mAvailableEffects = toArrayString(p.getSupportedColorEffects());
		mAvailableWhiteBalance = toArrayString(p.getSupportedWhiteBalance());
		mAvailableFlashMode = toArrayString(p.getSupportedFlashModes());
		mAvailablePictureSize = toArraySize(p.getSupportedPictureSizes());
		mAvailablePreviewSize = toArraySize(p.getSupportedPreviewSizes());
	}

	private String[] toArrayString(List<String> list) {
		return list.toArray(new String[list.size()]);
	}

	private Size[] toArraySize(List<Camera.Size> list) {
		Size s[] = new Size[list.size()];

		for (int i = 0; i < s.length; ++i) {
			s[i] = new Size(list.get(i));
		}

		return s;
	}

	public void setup() {
		Camera.Parameters params = mCamera.getParameters();

		params.setJpegQuality(mSettings.getQuality());

		if (!mSettings.getFlashMode().isEmpty()) {
			params.setFlashMode(mSettings.getFlashMode());
		}

		if (!mSettings.getBalance().isEmpty()) {
			params.setWhiteBalance(mSettings.getBalance());
		}

		if (!mSettings.getEffect().isEmpty()) {
			params.setColorEffect(mSettings.getEffect());
		}

		if (mSettings.getWidth() != 0 && mSettings.getHeight() != 0) {
			params.setPictureSize(mSettings.getWidth(), mSettings.getHeight());
			Camera.Size targetPreviewSize = getOptimalPreviewSize(params.getSupportedPreviewSizes(), mSettings.getWidth(), mSettings.getHeight());
			params.setPreviewSize(targetPreviewSize.width, targetPreviewSize.height);
			// resizePreviewView(targetPreviewSize.width, targetPreviewSize.height); // TODO
		}
		mCamera.setParameters(params);
		//mCamera.enableShutterSound(false);

		/*try {
			mCamera.reconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}

	public Camera getCamera() {
		return mCamera;
	}

	public void startPreview() {
		if (mCamera != null) {
			mCamera.startPreview();
		}
	}

	public void stopPreview() {
		if (mCamera != null) {
			mCamera.stopPreview();
		}
	}

	/**
	 * Compute optimal preview size for surface view from camera
	 * @param sizes available sizes from camera
	 * @param w width
	 * @param h height
	 * @return optimal size
	 */
	private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
		double targetRatio = ((double) w) / ((double) h);

		if (sizes == null) {
			return null;
		}

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;
		for (Camera.Size size : sizes) {
			if (Math.abs((((double) size.width) / ((double) size.height)) - targetRatio) <= 0.05d && ((double) Math.abs(size.height - h)) < minDiff) {
				optimalSize = size;
				minDiff = (double) Math.abs(size.height - h);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size2 : sizes) {
				if (((double) Math.abs(size2.height - h)) < minDiff) {
					optimalSize = size2;
					minDiff = (double) Math.abs(size2.height - h);
				}
			}
		}
		return optimalSize;
	}

	public Size[] getAvailablePictureSize() {
		return mAvailablePictureSize;
	}

	public Size[] getAvailablePreviewSize() {
		return mAvailablePreviewSize;
	}

	public String[] getAvailableEffects() {
		return mAvailableEffects;
	}

	public String[] getAvailableFlashMode() {
		return mAvailableFlashMode;
	}

	public String[] getAvailableWhiteBalance() {
		return mAvailableWhiteBalance;
	}

	public Size getCurrentPictureSize() {
		return new Size(mCamera.getParameters().getPictureSize());
	}

	public String getCurrentEffect() {
		return mCamera.getParameters().getColorEffect();
	}

	public String getCurrentFlashMode() {
		return mCamera.getParameters().getFlashMode();
	}

	public String getCurrentWhiteBalance() {
		return mCamera.getParameters().getWhiteBalance();
	}

	public void setEffect(String effect) {
		mCamera.getParameters().setColorEffect(effect);
	}

	public void setFlashMode(String mode) {
		mCamera.getParameters().setFlashMode(mode);
	}

	public void setWhiteBalance(String balance) {
		mCamera.getParameters().setWhiteBalance(balance);
	}

	public void release() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
		}
	}

	public void autoFocus(Camera.AutoFocusCallback listener) {
		mCamera.autoFocus(listener);
	}

	public void destroy() {
		mCamera = null;
	}

	public void setPreviewSize(int width, int height) {
		List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();

		for (Camera.Size previewSize : previewSizes) {
			float ratio = (((1.0f * ((float) width)) * ((float) previewSize.height)) / ((float) previewSize.width)) / ((float) height);
			if (((double) ratio) > 0.92d && ((double) ratio) < 1.02d) {
				mCamera.stopPreview();
				mCamera.getParameters().setPreviewSize(previewSize.width, previewSize.height);
				break;
			}
		}

		mCamera.getParameters().setPictureSize(width, height);
		mCamera.startPreview();
	}

	public void setQuality(int quality) {
		mCamera.getParameters().setJpegQuality(quality);
	}
}
