package ru.vlad805.timelapse.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.cameraview.CameraView;
import ru.vlad805.timelapse.Const;
import ru.vlad805.timelapse.PreferenceBundle;
import ru.vlad805.timelapse.R;
import ru.vlad805.timelapse.handler.DateTimeImageHandler;
import ru.vlad805.timelapse.handler.ImageHandler;
import ru.vlad805.timelapse.handler.LocationImageHandler;
import ru.vlad805.timelapse.handler.RotateImageHandler;
import ru.vlad805.timelapse.recorder.VideoRecorderNew;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TestActivity extends AppCompatActivity implements View.OnClickListener {

	private static final String TAG = TestActivity.class.getSimpleName();

	private PreferenceBundle mPrefs;
	private CameraView mCamera;

	private List<String> mFiles;
	private List<ImageHandler> mHandlers;
	private int mNumber = 0;

	private Timer mTimer;

	private State mCurrentState = State.THROTTLE;

	private enum State { THROTTLE, CAPTURING, PROCESSING }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		Bundle b = getIntent().getExtras();

		if (b == null) {
			return;
		}

		mPrefs = b.getParcelable(Intent.EXTRA_STREAM);

		createWorkingDirectory();

		initPreferences();
		initCamera();

		findViewById(R.id.camera_toggle).setOnClickListener(this);
	}

	@Override
	protected void onPause() {
		finishCapture();
		mCamera.stop();

		super.onPause();
	}

	private void initPreferences() {
		mHandlers = new ArrayList<>();
		List<Integer> tmp = mPrefs.getProcessingHandlers();
		if (tmp.size() > 0) {
			if (tmp.contains(Const.PROCESSING_HANDLER_DATETIME)) {
				mHandlers.add(new DateTimeImageHandler(mPrefs));
			}
			if (tmp.contains(Const.PROCESSING_HANDLER_ALIGN)) {
				mHandlers.add(new RotateImageHandler(mPrefs));
			}
			if (tmp.contains(Const.PROCESSING_HANDLER_GEOTRACK)) {
				mHandlers.add(new LocationImageHandler(mPrefs));
			}
		}

		long delay = mPrefs.getDelay();
		long interval = mPrefs.getInterval();

		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				doCapture();
			}
		}, delay, interval);

		for (ImageHandler handler : mHandlers) {
			handler.onCaptureStart();
		}
	}

	private void initCamera() {
		mFiles = new ArrayList<>();

		mCamera = findViewById(R.id.camera_surface);
		mCamera.start();
		mCamera.setOnPictureTakenListener(this::onPictureTaken);

		mCurrentState = State.CAPTURING;
		Log.i(TAG, "initCamera: inited");
	}

	private void onPictureTaken(Bitmap bitmap, int rotationDegrees) {
		/*Matrix matrix = new Matrix();
		matrix.postRotate(-rotationDegrees);
		savePhoto(Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true));*/
		PreferenceBundle.Resolution r = mPrefs.getResolution();
		Bitmap rszd = Bitmap.createScaledBitmap(bitmap, r.getWidth(), r.getHeight(), false);
		bitmap.recycle();
		savePhoto(rszd);
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void createWorkingDirectory() {
		File path = new File(mPrefs.getPath());

		if (path.exists()) {
			Log.i(TAG, "Path Exists: " + path.getAbsolutePath());
		} else {
			path.mkdirs();
			Log.i(TAG, "Create path success : " + path.getAbsolutePath());
		}
	}

	private void doCapture() {
		if (mCurrentState == State.CAPTURING) {
			mCamera.takePicture();
		}
	}

	private void savePhoto(Bitmap bitmap) {
		try {
			File f = new File(mPrefs.getPath() + File.separator + mNumber + ".jpg");
			FileOutputStream of = new FileOutputStream(f);
			bitmap.compress(Bitmap.CompressFormat.JPEG, mPrefs.getQuality(), of);
			of.flush();
			of.close();

			mFiles.add(f.getAbsolutePath());

			for (ImageHandler handler : mHandlers) {
				handler.onImageCaptured(mNumber);
			}

			++mNumber;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void finishCapture() {
		if (mCurrentState != State.CAPTURING) {
			return;
		}

		for (ImageHandler handler : mHandlers) {
			handler.onCaptureStop();
		}

		mTimer.cancel();

		startProcessing();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void startProcessing() {
		mCurrentState = State.PROCESSING;

		new Finalize().execute(mFiles.toArray(new String[0]));
	}

	private class Finalize extends AsyncTask<String, Integer, String> {

		private AlertDialog mDialog;
		private TextView mText;
		private TextView mNumbers;
		private ProgressBar mProgress;

		private static final int STATE_HANDLERS = 0;
		private static final int STATE_VIDEO = 1;

		private final int[] strings = new int[] {
				R.string.processing_handlers,
				R.string.processing_video
		};

		@Override
		protected void onPreExecute() {
			super.onPreExecute();


			View v = getLayoutInflater().inflate(R.layout.dialog_progress, null);
			AlertDialog.Builder dialog = new AlertDialog.Builder(TestActivity.this);
			dialog.setView(v);
			mText = v.findViewById(R.id.progress_text);
			mNumbers = v.findViewById(R.id.progress_numbers);
			mProgress = v.findViewById(R.id.progress_line);

			mDialog = dialog.create();
			mDialog.show();
		}

		@SuppressWarnings("ResultOfMethodCallIgnored")
		@Override
		protected String doInBackground(String... objects) {

			int size = objects.length;

			Log.i(TAG, "startProcessing: photos count = " + size);
			Log.i(TAG, "startProcessing: handlers count = " + mHandlers.size());

			int index = 0;

			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inMutable = true;

			for (String file : objects) {
				Bitmap bitmap = BitmapFactory.decodeFile(file, opt);

				for (ImageHandler ih : mHandlers) {
					ih.processImage(bitmap, index);
				}

				try {
					FileOutputStream of = new FileOutputStream(file);
					bitmap.compress(Bitmap.CompressFormat.JPEG, mPrefs.getQuality(), of);
					of.flush();
					of.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				publishProgress(STATE_HANDLERS, index, size);
				index++;
			}

			if (mPrefs.getRecordType() == Const.RECORD_TYPE_MP4) {
				PreferenceBundle.Resolution res = mPrefs.getResolution();
				VideoRecorderNew vr = new VideoRecorderNew(mPrefs.getPath(), "video.avi", res.getWidth(), res.getHeight(), mPrefs.getFps(), mPrefs.getQuality());

				index = 0;
				for (String file : objects) {
					vr.addFrame(BitmapFactory.decodeFile(file));
					publishProgress(STATE_VIDEO, index, size);
					new File(file).delete();
					index++;
				}
				vr.save();

			}

			return null;
		}

		@SuppressLint("DefaultLocale")
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);

			int type = values[0];
			int progress = values[1];
			int all = values[2];

			mText.setText(strings[type]);
			//mProgress.setProgress(100 * progress / all);
			mProgress.setProgress(progress);
			mProgress.setMax(all);
			mNumbers.setText(String.format("%d/%d", progress, all));
		}

		@Override
		protected void onPostExecute(String s) {
			super.onPostExecute(s);
			mDialog.cancel();
			Toast.makeText(TestActivity.this, "Hooray!", Toast.LENGTH_LONG).show();
		}
	}

	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.camera_toggle:
				finishCapture();
				break;
		}
	}
}
