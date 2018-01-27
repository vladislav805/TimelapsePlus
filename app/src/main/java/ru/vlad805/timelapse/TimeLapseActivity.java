package ru.vlad805.timelapse;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("deprecation")
public class TimeLapseActivity extends AppCompatActivity implements Callback, OnClickListener, PictureCallback {

	private static final boolean DEBUG = true;

	private static final String WORK_DIRECTORY = "/TimeLapseDir/";
	private static final String PREFS_EFFECT = "COLOREFFECT";
	private static final String PREFS_DELAY = "DELAY";
	private static final String PREFS_FPS = "FPS";
	private static final String PREFS_HEIGHT = "HEIGHT";
	private static final String PREFS_INTERVAL = "INTERVAL";
	private static final String PREFS_NAME = "TimeLapse";
	private static final String PREFS_QUALITY = "QUALITY";
	private static final String PREFS_WHITE_BALANCE = "WHITEBALANCE";
	private static final String PREFS_WIDTH = "WIDTH";
	private static final String PREFS_ZOOM = "ZOOM";

	private static final String TAG = "TimeLapse";

	private AudioManager mAudioManager;

	private WakeLock mWakeLock;
	private Camera mCamera;
	private SurfaceHolder mSurfaceHolder;
	private VideoRecorder mVideoRecorder;
	private Timer mTimer;
	private File mRoot;

	private SurfaceView mSurfaceView;
	private TextView mtvFramesCount;
	private FloatingActionButton mButtonToggle;

	private int mWidth = 0;
	private int mHeight = 0;
	private int mDelay;
	private int mFPS;
	private int mInterval;

	private String mBalance = "";
	private String mEffect = "";
	private String mPath = null;
	private String mTimeStamp;
	private int mQuality;

	private CaptureState mState = CaptureState.IDLE;

	/**
	 * Start activity
	 * @param savedInstanceState saved state
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);

		mPath = String.valueOf(Environment.getExternalStorageDirectory().getAbsolutePath()) + WORK_DIRECTORY;

		initDirectory();

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		mAudioManager = (AudioManager) getSystemService("audio");
		mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		mRoot = new File(mPath);

		initSurfaceView();
	}

	/**
	 * On minimize app, stop capturing
	 */
	public void onPause() {
		super.onPause();

		if (mState == CaptureState.RECORD) {
			stopCapture();
		}

		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}

	/**
	 * Click handler
	 * @param v view
	 */
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.startButton:
				if (mState == CaptureState.IDLE) {
					startCapture();
				} else {
					stopCapture();
				}
				break;

			case R.id.settingsButton:

				break;

			case R.id.openImageSettings:
				showPictureConfigDialog();
				break;

			case R.id.openVideoSettings:
				showTimeLapseConfigDialog();
				break;

			case R.id.mainSurface:
				mCamera.autoFocus(null);
				break;
		}
	}

	/**
	 * Listener on auto focus completed, capturing will started after *delay* ms
	 */
	private final AutoFocusCallback mStartCaptureAfterAutoFocus = new AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean b, Camera camera) {
			mTimer = new Timer();
			mTimer.schedule(new CaptureTask(), (long) mDelay);
		}
	};

	/**
	 * Create working directory if not exists
	 */
	private void initDirectory() {
		File path = new File(this.mPath);

		if (path.exists()) {
			debug("Path Exists: " + path.getAbsolutePath());
		} else {
			//noinspection ResultOfMethodCallIgnored
			path.mkdirs();
			debug("Create path success : " + path.getAbsolutePath());
		}
	}

	/**
	 * Set current count of frames in UI
	 */
	private void setCurrentCountOfFrames() {
		mtvFramesCount.setText(String.format(
				"Frames: %d; approx. %d sec.; size %d MB; free %d MB",
				//getString(R.string.mainFramesCount),
				mVideoRecorder.getFrameCount(),
				(int) (mVideoRecorder.getFrameCount() / mFPS),
				(int) (mVideoRecorder.getFileSize() / Math.pow(2, 20)),
				(int) (mRoot.getFreeSpace() / Math.pow(2, 20))
		));
	}

	/**
	 * Initialize surface view
	 */
	private void initSurfaceView() {
		debug("initSurfaceView");

		mSurfaceView = (SurfaceView) findViewById(R.id.mainSurface);
		mSurfaceView.setOnClickListener(this);

		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mSurfaceHolder.addCallback(this);

		mtvFramesCount = (TextView) findViewById(R.id.framesCount);
		mtvFramesCount.setText(R.string.mainFramesCountReady);

		mButtonToggle = (FloatingActionButton) findViewById(R.id.startButton);
		mButtonToggle.setOnClickListener(this);

		findViewById(R.id.openImageSettings).setOnClickListener(this);
		findViewById(R.id.openVideoSettings).setOnClickListener(this);
	}

	/**
	 * Fetch settings from shared prefs
	 */
	private void loadPreferences() {
		debug("Loading preference...");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		mEffect = settings.getString(PREFS_EFFECT, "");
		mWidth = settings.getInt(PREFS_WIDTH, 0);
		mHeight = settings.getInt(PREFS_HEIGHT, 0);
		mBalance = settings.getString(PREFS_WHITE_BALANCE, "");
		mDelay = settings.getInt(PREFS_DELAY, 3000);
		mInterval = settings.getInt(PREFS_INTERVAL, 5000);
		mFPS = settings.getInt(PREFS_FPS, 25);
		mQuality = settings.getInt(PREFS_QUALITY, 70);
	}

	/**
	 * Compute optimal preview size for surface view from camera
	 * @param sizes available sizes from camera
	 * @param w width
	 * @param h height
	 * @return optimal size
	 */
	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		double targetRatio = ((double) w) / ((double) h);

		if (sizes == null) {
			return null;
		}

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;
		for (Size size : sizes) {
			if (Math.abs((((double) size.width) / ((double) size.height)) - targetRatio) <= 0.05d && ((double) Math.abs(size.height - h)) < minDiff) {
				optimalSize = size;
				minDiff = (double) Math.abs(size.height - h);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size2 : sizes) {
				if (((double) Math.abs(size2.height - h)) < minDiff) {
					optimalSize = size2;
					minDiff = (double) Math.abs(size2.height - h);
				}
			}
		}
		return optimalSize;
	}

	/**
	 * Resize surface view
	 * @param camSize size of camera
	 */
	private void resizePreviewView(Size camSize) {
		View container = findViewById(R.id.linearLayoutPreview);

		if (container == null) {
			return;
		}

		int cw = container.getWidth(), ch = container.getHeight();
		int sw = camSize.width, sh = camSize.height;
		int pw, ph;
		double cc = ch / cw, sc = sh / sw, sp;

		if (cc > sc) {
			pw = sw;
			sp = 100 * sh / sw;
			ph = (int) (sp * cw / 100);
		} else {
			ph = sh;
			sp = 100 * sw / sh;
			pw = (int) (sp * ch / 100);
		}

		/*int newHeight = container.getHeight();
		int newWidth = (container.getHeight() * camSize.height) / camSize.width;

		if (newWidth > container.getWidth()) {
			newWidth = container.getWidth();
			newHeight = (container.getWidth() * camSize.width) / camSize.height;
		}*/

		LayoutParams lp = mSurfaceView.getLayoutParams();
		lp.width = pw;
		lp.height = ph;
		mSurfaceView.setLayoutParams(lp);
		debug("New size: " + lp.width + " x " + lp.height);
		mSurfaceView.requestLayout();
	}

	/**
	 * Callback for surface created
	 * @param holder holder
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		loadPreferences();
		debug("surfaceCreated.");
		mSurfaceHolder = holder;
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		try {
			mCamera = Camera.open();
			mCamera.setPreviewDisplay(holder);
			mCamera.setDisplayOrientation(0);
		} catch (Exception e) {
			if (mCamera != null) {
				mCamera.release();
			}
			Toast.makeText(this, "Fail to connect to camera, exiting...", Toast.LENGTH_LONG).show();
			mCamera = null;
			e.printStackTrace();
			finish();
			return;
		}

		if (mCamera == null) {
			Toast.makeText(this, "Fail to connect to camera, exiting...", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		Parameters params = mCamera.getParameters();
		params.setJpegQuality(mQuality);

		if (!mBalance.isEmpty()) {
			params.setWhiteBalance(mBalance);
		}

		if (!mEffect.isEmpty()) {
			params.setColorEffect(mEffect);
		}

		if (!(mWidth == 0 || mHeight == 0)) {
			params.setPictureSize(mWidth, mHeight);
			Size targetPreviewSize = getOptimalPreviewSize(params.getSupportedPreviewSizes(), mWidth, mHeight);
			params.setPreviewSize(targetPreviewSize.width, targetPreviewSize.height);
			resizePreviewView(targetPreviewSize);
		}
		mCamera.setParameters(params);
	}

	/**
	 * Callback for surface destroy
	 * @param holder holder in surface
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		debug("surfaceDestroyed.");
		savePreference();
		try {
			mCamera.stopPreview();
			mCamera.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		mCamera = null;
	}

	/**
	 * Callback for surface change sizes
	 * @param holder holder in surface
	 * @param format ?
	 * @param w new width
	 * @param h new height
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		debug("surfaceChanged: " + w + " x " + h);
		Parameters params = mCamera.getParameters();

		if (!mBalance.isEmpty()) {
			params.setWhiteBalance(mBalance);
		}

		if (!mEffect.isEmpty()) {
			params.setColorEffect(mEffect);
		}

		mCamera.setParameters(params);

		if (mCamera != null) {
			try {
				mCamera.startPreview();
			} catch (RuntimeException e) {
				Toast.makeText(this, "Fail to start preview, exiting...", Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}

	/**
	 * Start capture timelapse
	 */
	private void startCapture() {
		mButtonToggle.setImageResource(R.drawable.ic_stop);
		mWakeLock.acquire();
		mState = CaptureState.RECORD;

		mTimeStamp = getTimeStamp();

		Parameters param = mCamera.getParameters();
		Size size = param.getPictureSize();
		param.setRotation(0);
		mCamera.setParameters(param);

		String name = String.format("%s%s.avi", mPath, mTimeStamp);

		mVideoRecorder = new VideoRecorder(name, size.width, size.height, (double) mFPS);
		mCamera.autoFocus(mStartCaptureAfterAutoFocus);
	}

	/**
	 * Stop capture timelapse
	 */
	private void stopCapture() {
		mButtonToggle.setImageResource(R.drawable.ic_videocam);
		mWakeLock.release();
		mState = CaptureState.IDLE;
		mtvFramesCount.setText(R.string.mainFramesCountFinished);

		try {
			mTimer.cancel();
			mTimer = null;
		} catch (IllegalStateException e) {
			mTimer = null;
		}
		if (mVideoRecorder != null) {
			mVideoRecorder.close();
		}
		mVideoRecorder = null;
	}

	/**
	 * Timer task for capture frame
	 */
	public class CaptureTask extends TimerTask {

		public void run() {
			mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
			mCamera.takePicture(null, null, null, TimeLapseActivity.this);
		}
	}

	/**
	 * Callback, called by camera after frame was captured
	 * @param data array of bytes
	 * @param camera camera
	 */
	public void onPictureTaken(byte[] data, Camera camera) {
		debug("jpeg picture taken");
		if (mVideoRecorder != null) {
			mVideoRecorder.addFrame(data);

			setCurrentCountOfFrames();

			/*if (mVideoRecorder.getFrameCount() == 1) {
				savePicture(String.format("%s%s.jpg", mPath, mTimeStamp), data);
			}*/
		}
		if (mCamera != null) {
			mCamera.startPreview();
		}
		mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
		if (mTimer != null) {
			mTimer.schedule(new CaptureTask(), (long) mInterval);
		}
	}

	/**
	 * @return string with date in human style
	 */
	public String getTimeStamp() {
		Calendar c = Calendar.getInstance();
		return String.format(Locale.getDefault(), "%04d%02d%02d%02d%02d%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(5), c.get(11), c.get(12), c.get(13));
	}

	/**
	 * Save preview for video
	 * @deprecated do not use
	 * @param filename file name of frame
	 * @param data bytes of contents file
	 */
	private void savePicture(String filename, byte[] data) {
		try (FileOutputStream fos = new FileOutputStream(filename)) {
			fos.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initSpinnerFilter(final Parameters params, Spinner spinnerFilter) {
		String curEffect = params.getColorEffect();
		final List<String> effectList = params.getSupportedColorEffects();
		if (effectList != null) {
			ArrayAdapter<String> filterArrayAdapter = new ArrayAdapter<>(this, 17367048, effectList.toArray(new String[effectList.size()]));
			filterArrayAdapter.setDropDownViewResource(17367049);
			spinnerFilter.setAdapter(filterArrayAdapter);
			for (int i = 0; i < effectList.size(); i++) {
				if (curEffect.equals(effectList.get(i))) {
					spinnerFilter.setSelection(i);
				}
			}
			spinnerFilter.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
					TimeLapseActivity.this.mEffect = effectList.get(position);
					params.setColorEffect(TimeLapseActivity.this.mEffect);
					setCameraParams(params);
				}

				public void onNothingSelected(AdapterView<?> adapterView) {
				}
			});
		}
	}

	private void initSpinnerSize(Parameters param, Spinner spinnerSize) {
		final List<Size> sizeList = param.getSupportedPictureSizes();
		final List<Size> previewSizes = param.getSupportedPreviewSizes();

		final Size curSize = param.getPictureSize();

		if (curSize != null && sizeList != null) {
			int i;
			debug("--- Current Picture Size: " + curSize.width + " x " + curSize.height);
			String[] sizeArray = new String[sizeList.size()];

			for (i = 0; i < sizeList.size(); i++) {
				sizeArray[i] = sizeList.get(i).width + " x " + sizeList.get(i).height;
			}

			ArrayAdapter<String> sizeArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sizeArray);
			sizeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinnerSize.setAdapter(sizeArrayAdapter);
			i = 0;

			while (i < sizeList.size()) {
				if (curSize.width == sizeList.get(i).width && curSize.height == sizeList.get(i).height) {
					spinnerSize.setSelection(i);
				}
				i++;
			}

			final Parameters parameters = param;
			spinnerSize.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
					Size targetSize = sizeList.get(position);
					mWidth = targetSize.width;
					mHeight = targetSize.height;
					if (targetSize.width == curSize.width && targetSize.height == curSize.height) {
						debug("No need to change!");
						return;
					}
					for (Size previewSize : previewSizes) {
						float ratio = (((1.0f * ((float) targetSize.width)) * ((float) previewSize.height)) / ((float) previewSize.width)) / ((float) targetSize.height);
						if (((double) ratio) > 0.92d && ((double) ratio) < 1.02d) {
							mCamera.stopPreview();
							parameters.setPreviewSize(previewSize.width, previewSize.height);
							break;
						}
					}
					resizePreviewView(targetSize);
					parameters.setPictureSize(targetSize.width, targetSize.height);
					setCameraParams(parameters);
					mCamera.startPreview();
				}

				public void onNothingSelected(AdapterView<?> adapterView) {
				}
			});
		}
	}

	private void initSpinnerWhiteBalance(final Parameters params, Spinner spinnerBalance) {
		String curBalance = params.getWhiteBalance();

		final List<String> balanceList = params.getSupportedWhiteBalance();

		if (balanceList != null) {
			ArrayAdapter<String> balanceArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, balanceList.toArray(new String[balanceList.size()]));
			balanceArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinnerBalance.setAdapter(balanceArrayAdapter);

			for (int i = 0; i < balanceList.size(); i++) {
				if (curBalance.equals(balanceList.get(i))) {
					spinnerBalance.setSelection(i);
				}
			}

			spinnerBalance.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
					mBalance = balanceList.get(position);
					params.setWhiteBalance(TimeLapseActivity.this.mBalance);
					setCameraParams(params);
				}

				public void onNothingSelected(AdapterView<?> adapterView) {
				}
			});
		}
	}

	private void setCameraParams(Parameters params) {
		try {
			mCamera.setParameters(params);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showTimeLapseConfigDialog() {
		View layout = ((LayoutInflater) getSystemService("layout_inflater")).inflate(R.layout.activity_settings_video, null);

		final EditText editTextDelay = layout.findViewById(R.id.editTextDelay);
		editTextDelay.setText(String.valueOf(mDelay));

		final EditText editTextInterval = layout.findViewById(R.id.editTextInterval);
		editTextInterval.setText(String.valueOf(mInterval));

		final EditText editTextFPS = layout.findViewById(R.id.editTextFPS);
		editTextFPS.setText(String.valueOf(mFPS));

		final SeekBar seekFPS = layout.findViewById(R.id.editSeekFPS);
		seekFPS.setProgress(mFPS - 15);
		seekFPS.setMax(45);
		seekFPS.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				editTextFPS.setText(String.valueOf(seekBar.getProgress() + 15));
			}

			@Override public void onStartTrackingTouch(SeekBar seekBar) { }
			@Override public void onStopTrackingTouch(SeekBar seekBar) { }
		});

		new Builder(this)
				.setTitle(R.string.settingsTitle)
				.setView(layout)
				.setPositiveButton(R.string.settingsSave, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						mDelay = getIntegerValue(editTextDelay, 3000);
						mInterval = getIntegerValue(editTextInterval, 5000);
						mFPS = getIntegerValue(editTextFPS, 15);
						mFPS = Math.max(15, mFPS);
						mFPS = Math.min(60, mFPS);
						savePreference();
					}
				})
				.create()
				.show();
	}

	private void showPictureConfigDialog() {
		Parameters param = mCamera.getParameters();

		View layout = ((LayoutInflater) getSystemService("layout_inflater")).inflate(R.layout.activity_settings_image, null);

		final SeekBar seekQuality = layout.findViewById(R.id.editTextQuality);
		seekQuality.setProgress(mQuality);
		seekQuality.setMax(100);

		AlertDialog dialog = new Builder(this)
				.setTitle(R.string.settingsTitle)
				.setView(layout)
				.setPositiveButton(R.string.settingsSave, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						mQuality = seekQuality.getProgress();
						savePreference();
					}
				}).create();
		initSpinnerFilter(param, (Spinner) layout.findViewById(R.id.spinnerFilter));
		initSpinnerSize(param, (Spinner) layout.findViewById(R.id.spinnerSize));
		initSpinnerWhiteBalance(param, (Spinner) layout.findViewById(R.id.spinnerWhiteBalance));
		dialog.show();
	}

	private int getIntegerValue(EditText inputBox, int defaultValue) {
		int output = defaultValue;
		try {
			output = Integer.parseInt(inputBox.getText().toString());
		} catch (Exception ignored) {
		}
		return output;
	}

	/**
	 * Quit dialog
	 */
	private void showQuitDialog() {
		new Builder(this)
				.setTitle(R.string.exitDialogTitle)
				.setMessage(R.string.exitDialogQuestion)
				.setPositiveButton(R.string.exitDialogOK, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						finish();
					}
				})
				.setNegativeButton(R.string.exitDialogCancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) { }
				})
				.create()
				.show();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == 4) {
			showQuitDialog();
			return DEBUG;
		} else if (keyCode != 82) {
			return super.onKeyDown(keyCode, event);
		} else {
			event.startTracking();
			return DEBUG;
		}
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == 82 && event.isTracking() && !event.isCanceled()) {
			openOptionsMenu();
		}
		return super.onKeyUp(keyCode, event);
	}

	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == 82) {
			return DEBUG;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Save settings
	 */
	private void savePreference() {
		debug("Saving preference...");
		Parameters params = mCamera.getParameters();
		Editor editor = getSharedPreferences(TAG, Context.MODE_PRIVATE).edit();
		editor.putString(PREFS_EFFECT, params.getColorEffect());
		Size size = params.getPictureSize();
		if (size != null) {
			editor.putInt(PREFS_WIDTH, size.width);
			editor.putInt(PREFS_HEIGHT, size.height);
		}
		editor.putString(PREFS_WHITE_BALANCE, params.getWhiteBalance());
		editor.putInt(PREFS_ZOOM, params.getZoom());
		editor.putInt(PREFS_DELAY, mDelay);
		editor.putInt(PREFS_INTERVAL, mInterval);
		editor.putInt(PREFS_FPS, mFPS);
		editor.putInt(PREFS_QUALITY, mQuality);
		editor.apply();
	}



	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 1 && resultCode == 999) {
			finish();
		}
	}


	private void debug(String msg) {
		Log.e("TimeLapse", msg);
	}
}
