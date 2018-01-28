package ru.vlad805.timelapse;

import android.Manifest;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("deprecation")
public class TimeLapseActivity extends AppCompatActivity implements Callback, OnClickListener, PictureCallback, SettingsDialog.OnSettingsChanged, BatteryReceiver.OnBatteryLevelChangedListener {

	private static final boolean DEBUG = true;

	private static final String TAG = "TimeLapse";

	private AudioManager mAudioManager;

	private WakeLock mWakeLock;
	private Camera mCamera;
	private SurfaceHolder mSurfaceHolder;
	private Timer mTimer;
	private File mRoot;

	private IRecorder mVideoRecorder = null;
	private IImageHandler mImageHandler = null;

	private SurfaceView mSurfaceView;
	private TextView mtvFramesCount;
	private TextView mtvPrefsCapture;
	private TextView mtvBatteryLevel;
	private FloatingActionButton mButtonToggle;

	private SettingsBundle mSettings;
	private BatteryReceiver mBattery;

	private CaptureState mState = CaptureState.IDLE;

	/**
	 * Start activity
	 * @param savedInstanceState saved state
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		mAudioManager = (AudioManager) getSystemService("audio");
		mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		mSettings = new SettingsBundle(this).load();
		mRoot = new File(mSettings.getPath());
		initDirectory();

		checkIntro();

		initSurfaceView();

		mBattery = new BatteryReceiver(this);
		registerReceiver(mBattery, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}

	/**
	 * On minimize app, stop capturing
	 */
	@Override
	public void onPause() {
		super.onPause();

		if (mState == CaptureState.RECORD) {
			stopCapture();
		}

		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mBattery);

		mBattery = null;
		mWakeLock = null;

		super.onDestroy();
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
				openSettings();
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
			mTimer.schedule(new CaptureTask(), (long) mSettings.getDelay());
		}
	};

	/**
	 * Create working directory if not exists
	 */
	private void initDirectory() {
		File path = new File(mSettings.getPath());

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
		//noinspection RedundantCast
		mtvFramesCount.setText(String.format(
				getString(R.string.mainFramesCount),
				mVideoRecorder.getFrameCount(),
				(int) (mVideoRecorder.getFrameCount() / mSettings.getFPS()),
				(int) (mVideoRecorder.getFileSize() / Math.pow(2, 20)),
				(int) (mRoot.getFreeSpace() / Math.pow(2, 20))
		));
	}

	private void setCurrentSettingsPreview() {
		Size s = mCamera.getParameters().getPictureSize();
		mtvPrefsCapture.setText(String.format(
				getString(R.string.mainMediaInfo),
				s.width,
				s.height,
				mSettings.getFPS(),
				mSettings.getInterval()
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

		mtvPrefsCapture = (TextView) findViewById(R.id.framesPrefs);

		mButtonToggle = (FloatingActionButton) findViewById(R.id.startButton);
		mButtonToggle.setOnClickListener(this);

		mtvBatteryLevel = (TextView) findViewById(R.id.mainBatteryLevel);

		findViewById(R.id.settingsButton).setOnClickListener(this);
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
	 */
	private void resizePreviewView(int sw, int sh) {
		View container = findViewById(R.id.linearLayoutPreview);

		if (container == null) {
			return;
		}

		int cw = container.getWidth(), ch = container.getHeight();
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
		params.setJpegQuality(mSettings.getQuality());

		if (!mSettings.getBalance().isEmpty()) {
			params.setWhiteBalance(mSettings.getBalance());
		}

		if (!mSettings.getEffect().isEmpty()) {
			params.setColorEffect(mSettings.getEffect());
		}

		if (!(mSettings.getWidth() == 0 || mSettings.getHeight() == 0)) {
			params.setPictureSize(mSettings.getWidth(), mSettings.getHeight());
			Size targetPreviewSize = getOptimalPreviewSize(params.getSupportedPreviewSizes(), mSettings.getWidth(), mSettings.getHeight());
			params.setPreviewSize(targetPreviewSize.width, targetPreviewSize.height);
			resizePreviewView(targetPreviewSize.width, targetPreviewSize.height);
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

		if (!mSettings.getBalance().isEmpty()) {
			params.setWhiteBalance(mSettings.getBalance());
		}

		if (!mSettings.getEffect().isEmpty()) {
			params.setColorEffect(mSettings.getEffect());
		}

		if (!mSettings.getFlashMode().isEmpty()) {
			params.setFlashMode(mSettings.getFlashMode());
		}

		setupImageHandler();

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

	private int mPreviousBrightness;

	/**
	 * Start capture timelapse
	 */
	private void startCapture() {
		mButtonToggle.setImageResource(R.drawable.ic_stop);
		mWakeLock.acquire();
		mState = CaptureState.RECORD;

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
			mPreviousBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
			Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
		}

		Parameters param = mCamera.getParameters();
		Size size = param.getPictureSize();
		param.setRotation(0);
		mCamera.setParameters(param);

		setCurrentSettingsPreview();

		switch (mSettings.getRecordMode()) {
			case Setting.RecordMode.VIDEO:
				mVideoRecorder = new VideoRecorder(mSettings.getPath(), String.format("%s.avi", getTimeStamp()), size.width, size.height, (double) mSettings.getFPS());
				break;

			case Setting.RecordMode.PHOTO_DIR:
				mVideoRecorder = new PictureRecorder(mSettings.getPath(), getTimeStamp());
		}

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

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
			Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, mPreviousBrightness);
		}

		try {
			mTimer.cancel();
			mTimer = null;
		} catch (IllegalStateException e) {
			mTimer = null;
		}
		if (mVideoRecorder != null) {
			mVideoRecorder.stop();
		}
		mVideoRecorder = null;
	}

	@Override
	public void onVideoPreferencesChanged() {
		Log.i(TAG, "onVideoPreferencesChanged: ");
		setCurrentSettingsPreview();
	}

	@Override
	public void onImagePreferencesChanged() {
		Log.i(TAG, "onImagePreferencesChanged: ");
		Parameters p = mCamera.getParameters();

		p.setFlashMode(mSettings.getFlashMode());
		p.setColorEffect(mSettings.getEffect());
		p.setWhiteBalance(mSettings.getBalance());
		p.setPictureSize(mSettings.getWidth(), mSettings.getHeight());

		setupImageHandler();

		try {
			mCamera.reconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

		mCamera.setParameters(p);
	}

	@Override
	public void onSizeChanged(int width, int height) {
		Log.i(TAG, "onSizeChanged: ");

		List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();

		for (Camera.Size previewSize : previewSizes) {
			float ratio = (((1.0f * ((float) width)) * ((float) previewSize.height)) / ((float) previewSize.width)) / ((float) height);
			if (((double) ratio) > 0.92d && ((double) ratio) < 1.02d) {
				mCamera.stopPreview();
				mCamera.getParameters().setPreviewSize(previewSize.width, previewSize.height);
				break;
			}
		}
		resizePreviewView(width, height);
		mCamera.getParameters().setPictureSize(width, height);
		mCamera.startPreview();
	}

	@Override
	public void onDirectoryChanged(String path) {
		Log.i(TAG, "onDirectoryChanged: ");
		initDirectory();
	}

	@Override
	public void onBatteryLevelChanged(int level) {
		Log.i(TAG, "onBatteryLevelChanged: " + level);
		mtvBatteryLevel.setText(String.format(getString(R.string.mainBatteryLevel), level));
		mtvBatteryLevel.setTextColor(level > 15 ? Color.argb(127, 255, 255, 255) : Color.RED);
	}

	/**
	 * Timer task for capture frame
	 */
	public class CaptureTask extends TimerTask {

		public void run() {
			toggleAudioMute(true);
			mCamera.takePicture(null, null, null, TimeLapseActivity.this);
		}
	}

	private void toggleAudioMute(boolean state) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			mAudioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, state ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
		} else {
			mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, state);
		}
	}

	private void setupImageHandler() {
		if (mImageHandler != null && mSettings.getImageHandler() == mImageHandler.getId()) {
			return;
		} else {
			if (mImageHandler != null) {
				mImageHandler.destroy();
			}
		}

		switch (mSettings.getImageHandler()) {
			case Setting.ImageHandler.NONE:
				mImageHandler = new StandartImageHandler();
				break;

			case Setting.ImageHandler.INSERT_DATE_AND_TIME:
				mImageHandler = new DateTimeImageHandler(mSettings);
				break;
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
			mVideoRecorder.addFrame(mImageHandler.handle(data).getBytes());
			setCurrentCountOfFrames();
		}
		if (mCamera != null) {
			mCamera.startPreview();
		}
		toggleAudioMute(false);
		if (mTimer != null) {
			mTimer.schedule(new CaptureTask(), (long) mSettings.getInterval());
		}
	}

	/**
	 * @return string with date in human style
	 */
	public String getTimeStamp() {
		Calendar c = Calendar.getInstance();
		return String.format(Locale.getDefault(), "%04d%02d%02d%02d%02d%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(5), c.get(11), c.get(12), c.get(13));
	}

	private void openSettings() {
		new SettingsDialog(this, mSettings, mCamera)
				.setOnSettingsChanged(this)
				.open();
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
				.setNegativeButton(R.string.exitDialogCancel, null)
				.create()
				.show();
	}

	private void checkIntro() {
		if ((mSettings.getIntro() & Setting.INTRO_ABOUT_PLAYING) == 0) {
			new Builder(this)
					.setTitle(R.string.warnPlayingTitle)
					.setMessage(R.string.warnPlayingContent)
					.setPositiveButton(R.string.warnPlayingOK, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							mSettings.setIntro(mSettings.getIntro() | Setting.INTRO_ABOUT_PLAYING);
						}
					})
					.create()
					.show();
		}
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
		mSettings.setBalance(params.getWhiteBalance()).setEffect(params.getColorEffect());

		Size size = params.getPictureSize();
		if (size != null) {
			mSettings.setWidth(size.width).setHeight(size.height);
		}

		mSettings.save();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		switch (requestCode) {
			case 1:
				if (resultCode == 999) {
					finish();
				}
				break;
		}
	}

	private void debug(String msg) {
		Log.e("TimeLapse", msg);
	}
}
