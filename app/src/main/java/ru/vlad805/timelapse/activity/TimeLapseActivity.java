package ru.vlad805.timelapse.activity;

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
import android.hardware.Camera.PictureCallback;
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
import ru.vlad805.timelapse.*;
import ru.vlad805.timelapse.control.TLPServer;
import ru.vlad805.timelapse.imagehandler.DateTimeImageHandler;
import ru.vlad805.timelapse.imagehandler.IImageHandler;
import ru.vlad805.timelapse.imagehandler.StandardImageHandler;
import ru.vlad805.timelapse.recorder.IRecorder;
import ru.vlad805.timelapse.recorder.PictureRecorder;
import ru.vlad805.timelapse.recorder.VideoRecorder;

import java.io.File;
import java.util.*;

@SuppressWarnings("deprecation")
public class TimeLapseActivity extends AppCompatActivity implements Callback, OnClickListener, PictureCallback, SettingsDialog.OnSettingsChanged, BatteryReceiver.OnBatteryLevelChangedListener {

	private static final boolean DEBUG = true;

	private static final String TAG = "TimeLapse";

	private WakeLock mWakeLock;
	private CameraAdapter mCameraAdapter;
	private SurfaceHolder mSurfaceHolder;
	private Timer mTimer;
	private File mRoot;

	private IRecorder mVideoRecorder = null;
	private TLPServer mRemoteControl = null;
	private IImageHandler mImageHandler = null;

	private SurfaceView mSurfaceView;
	private TextView mtvFramesCount;
	private TextView mtvPrefsCapture;
	private TextView mtvBatteryLevel;
	private FloatingActionButton mButtonToggle;

	private SettingsBundle mSettings;
	private BatteryReceiver mBattery;

	private CaptureState mState = CaptureState.IDLE;

	private int mPreviousBrightness;


	// private Server mServer;

	// private byte[] mLastCapture = null;

	/**
	 * Start activity
	 * @param savedInstanceState saved state
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
		mSettings = new SettingsBundle(this).load();
		mRoot = new File(mSettings.getPath());
		mCameraAdapter = new CameraAdapter(this, mSettings);

		checkIntro();
		initDirectory();
		initGraphicalUserInterface();
		initRemoteControl();

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

		mCameraAdapter.stopPreview();

		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mBattery);

		mCameraAdapter.destroy();

		mBattery = null;
		mWakeLock = null;

		if (mRemoteControl != null) {
			mRemoteControl.stop();
		}

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
				mCameraAdapter.autoFocus(null);
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

	public void updateSettingsPreview() {
		CameraAdapter.Size s = mCameraAdapter.getCurrentPictureSize();
		mtvPrefsCapture.setText(String.format(
				getString(R.string.mainMediaInfo),
				s.getWidth(),
				s.getHeight(),
				mSettings.getFPS(),
				mSettings.getInterval()
		));
	}

	/**
	 * Initialize GUI
	 */
	private void initGraphicalUserInterface() {
		debug("initGraphicalUserInterface");

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

	private void initRemoteControl() {
		if (mSettings.hasRemoteControl()) {
			if (mRemoteControl == null) {
				mRemoteControl = new TLPServer(mCameraAdapter, mSettings, this);
				mRemoteControl.start();
			}
		} else {
			if (mRemoteControl != null) {
				mRemoteControl.stop();
			}
			mRemoteControl = null;
		}
	}

	/**
	 * Resize surface view
	 * TODO replace it
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

		Camera camera = null;
		try {
			camera = Camera.open();
			mCameraAdapter.setCamera(camera);
			camera.setPreviewDisplay(holder);
			camera.startPreview();
			//mCamera.setDisplayOrientation(0);
		} catch (Exception e) {
			if (camera != null) {
				camera.release();
			}
			Toast.makeText(this, "Fail to connect to camera, exiting...", Toast.LENGTH_LONG).show();
			e.printStackTrace();
			finish();
			return;
		}

		mCameraAdapter.setup();
		mCameraAdapter.startPreview();
	}

	/**
	 * Callback for surface destroy
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		debug("surfaceDestroyed.");
		savePreference();
		try {
			mCameraAdapter.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Callback for surface change sizes
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		debug("surfaceChanged: " + w + " x " + h);

		mCameraAdapter.setup();

		setupImageHandler();
	}

	private void toggleLessBrightness(boolean state) {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
			if (state) {
				mPreviousBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
			}
			Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, state ? 0 : mPreviousBrightness);
		}
	}

	/**
	 * Start capture timelapse
	 */
	private void startCapture() {
		mButtonToggle.setImageResource(R.drawable.ic_stop);
		mWakeLock.acquire();
		mState = CaptureState.RECORD;

		toggleLessBrightness(true);

		updateSettingsPreview();

		switch (mSettings.getRecordMode()) {
			case Setting.RecordMode.VIDEO:
				CameraAdapter.Size size = mCameraAdapter.getCurrentPictureSize();
				mVideoRecorder = new VideoRecorder(mSettings.getPath(), String.format("%s.avi", getTimeStamp()), size.getWidth(), size.getHeight(), (double) mSettings.getFPS());
				break;

			case Setting.RecordMode.PHOTO_DIR:
				mVideoRecorder = new PictureRecorder(mSettings.getPath(), getTimeStamp());
				break;

			default:
				debug("startCapture: WTF, unknown record mode = " + mSettings.getRecordMode());
		}
		mCameraAdapter.autoFocus(mStartCaptureAfterAutoFocus);
	}

	/**
	 * Stop capture timelapse
	 */
	private void stopCapture() {
		mButtonToggle.setImageResource(R.drawable.ic_videocam);
		mWakeLock.release();
		mState = CaptureState.IDLE;
		mtvFramesCount.setText(R.string.mainFramesCountFinished);

		toggleLessBrightness(false);

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
		debug("onVideoPreferencesChanged");
		updateSettingsPreview();
	}

	@Override
	public void onImagePreferencesChanged() {
		debug("onImagePreferencesChanged");

		setupImageHandler();
	}

	@Override
	public void onSizeChanged(int width, int height) {
		Log.i(TAG, "onSizeChanged: ");

		mCameraAdapter.setPreviewSize(width, height);

		resizePreviewView(width, height);
	}

	@Override
	public void onDirectoryChanged(String path) {
		Log.i(TAG, "onDirectoryChanged: ");
		initDirectory();
	}

	@Override
	public void onControlChanged() {
		Log.e(TAG, "onControlChanged: onControlChanged");
		initRemoteControl();
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


		// WTF: onPictureTaken don't called if call takePicture apply in adapter
		public void run() {
			mCameraAdapter.getCamera().takePicture(null, null, null, TimeLapseActivity.this);
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
				mImageHandler = new StandardImageHandler();
				break;

			case Setting.ImageHandler.INSERT_DATE_AND_TIME:
				mImageHandler = new DateTimeImageHandler(mSettings);
				break;
		}
	}

	/**
	 * Callback, called by camera after frame was captured
	 */
	public void onPictureTaken(byte[] data, Camera camera) {
		debug("jpeg picture taken");
		if (mVideoRecorder != null) {
			byte[] s = mImageHandler.handle(data).getBytes();
			mVideoRecorder.addFrame(s);
			setCurrentCountOfFrames();
			if (mRemoteControl != null) {
				mRemoteControl.setLastCapture(s);
			}
		}
		mCameraAdapter.startPreview();
		if (mTimer != null) {
			mTimer.schedule(new CaptureTask(), (long) mSettings.getInterval());
		}
	}

	/**
	 * @return string with date in human style
	 */
	public String getTimeStamp() {
		Calendar c = Calendar.getInstance();
		return String.format(Locale.getDefault(), "%04d%02d%02d%02d%02d%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
	}

	private void openSettings() {
		new SettingsDialog(this, mSettings, mCameraAdapter)
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

		CameraAdapter.Size size;
		if ((size = mCameraAdapter.getCurrentPictureSize()) != null) {
			mSettings.setWidth(size.getWidth()).setHeight(size.getHeight());
		}

		mSettings.save();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		/*switch (requestCode) { // TODO WTF?
			case 1:
				if (resultCode == 999) {
					finish();
				}
				break;
		}*/
	}

	private <T> void debug(T msg) {
		Log.e("TimeLapse", String.valueOf(msg));
	}
}
