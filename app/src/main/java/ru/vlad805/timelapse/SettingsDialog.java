package ru.vlad805.timelapse;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import java.util.List;

@SuppressWarnings("deprecation")
public class SettingsDialog implements SeekBar.OnSeekBarChangeListener, DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener, DialogInterface.OnCancelListener, View.OnKeyListener {

	private static final String TAG = "Settings";
	private Context mContext;
	private SettingsBundle mSettings;
	private Camera mCamera;

	private View mRoot;
	private EditText editTextDelay;
	private EditText editTextInterval;
	private EditText editTextFPS;
	private SeekBar seekFPS;
	private SeekBar seekQuality;
	private EditText editTextPath;

	private List<String> mEffectsList;

	private List<String> mBalancesList;

	private List<Camera.Size> mSizesList;

	private List<String> mFlashesList;

	private int mRecordMode[] = {
			Setting.RecordMode.VIDEO,
			Setting.RecordMode.PHOTO_DIR
	};

	private int mImageHandler[] = {
			Setting.ImageHandler.NONE,
			Setting.ImageHandler.INSERT_DATE_AND_TIME
	};

	private OnSettingsChanged mOnSettingsChanged = null;

	@SuppressWarnings("UnnecessaryInterfaceModifier")
	public interface OnSettingsChanged {
		public void onVideoPreferencesChanged();
		public void onImagePreferencesChanged();
		public void onDirectoryChanged(String path);
		public void onSizeChanged(int width, int height);
	}

	public SettingsDialog(Context context, SettingsBundle settings, Camera camera) {
		mContext = context;
		mSettings = settings;
		mCamera = camera;
	}

	public void open() {
		mRoot = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.activity_settings, null);

		TabHost tabHost = mRoot.findViewById(R.id.settingsTabsHost);
		tabHost.setup();

		tabHost.addTab(tabHost.newTabSpec("image").setContent(R.id.settingsTabImage).setIndicator(mContext.getString(R.string.settingTabImage)));
		tabHost.addTab(tabHost.newTabSpec("video").setContent(R.id.settingsTabVideo).setIndicator(mContext.getString(R.string.settingTabVideo)));
		tabHost.addTab(tabHost.newTabSpec("about").setContent(R.id.settingsTabAbout).setIndicator(mContext.getString(R.string.settingTabAbout)));

		editTextDelay = mRoot.findViewById(R.id.editTextDelay);
		editTextDelay.setText(String.valueOf(mSettings.getDelay()));

		editTextInterval = mRoot.findViewById(R.id.editTextInterval);
		editTextInterval.setText(String.valueOf(mSettings.getInterval()));

		editTextFPS = mRoot.findViewById(R.id.editTextFPS);
		editTextFPS.setText(String.valueOf(mSettings.getFPS()));
		editTextFPS.setOnKeyListener(this);

		seekFPS = mRoot.findViewById(R.id.editSeekFPS);
		seekFPS.setProgress(mSettings.getFPS() - 15);
		seekFPS.setMax(45);
		seekFPS.setOnSeekBarChangeListener(this);

		seekQuality = mRoot.findViewById(R.id.editTextQuality);
		seekQuality.setProgress(mSettings.getQuality());
		seekQuality.setMax(100);

		editTextPath = mRoot.findViewById(R.id.editTextPath);
		editTextPath.setText(mSettings.getPath());

		((TextView) mRoot.findViewById(R.id.aboutVersion)).setText(String.format(mContext.getString(R.string.aboutVersion), BuildConfig.VERSION_NAME));

		initSpinnerFilter((Spinner) mRoot.findViewById(R.id.spinnerFilter));
		initSpinnerSize((Spinner) mRoot.findViewById(R.id.spinnerSize));
		initSpinnerWhiteBalance((Spinner) mRoot.findViewById(R.id.spinnerWhiteBalance));
		initSpinnerFlash((Spinner) mRoot.findViewById(R.id.spinnerFlash));
		initSpinnerRecordMove((Spinner) mRoot.findViewById(R.id.spinnerMode));
		initSpinnerImageHandler((Spinner) mRoot.findViewById(R.id.spinnerHandler));

		new Builder(mContext).setView(mRoot).setPositiveButton(R.string.settingsSave, this).setOnCancelListener(this).create().show();
	}

	private int getIntegerValue(EditText inputBox, int defaultValue) {
		int output = defaultValue;
		try {
			output = Integer.parseInt(inputBox.getText().toString());
		} catch (Exception ignored) {
		}
		return output;
	}

	private void initSpinnerFilter(Spinner spinner) {
		String curEffect = mCamera.getParameters().getColorEffect();
		mEffectsList = mCamera.getParameters().getSupportedColorEffects();

		if (mEffectsList == null) {
			mRoot.findViewById(R.id.settingsRowFilter).setVisibility(View.GONE);
			return;
		}

		initBaseSpinner(spinner, mEffectsList.toArray(new String[mEffectsList.size()]));
		for (int i = 0; i < mEffectsList.size(); i++) {
			if (curEffect.equals(mEffectsList.get(i))) {
				spinner.setSelection(i);
			}
		}

		spinner.setOnItemSelectedListener(this);
	}

	private void initSpinnerSize(Spinner spinner) {
		Camera.Size curSize = mCamera.getParameters().getPictureSize();
		mSizesList = mCamera.getParameters().getSupportedPictureSizes();

		if (curSize != null && mSizesList != null) {
			String[] sizeArray = new String[mSizesList.size()];
			for (int i = 0; i < mSizesList.size(); i++) {
				sizeArray[i] = mSizesList.get(i).width + "x" + mSizesList.get(i).height;
			}


			initBaseSpinner(spinner, sizeArray);
			for (int i = 0; i < mSizesList.size(); ++i) {
				if (curSize.width == mSizesList.get(i).width && curSize.height == mSizesList.get(i).height) {
					spinner.setSelection(i);
					break;
				}
			}
			spinner.setOnItemSelectedListener(this);
		}
	}

	private void initSpinnerWhiteBalance(Spinner spinner) {
		String curBalance = mCamera.getParameters().getWhiteBalance();
		mBalancesList = mCamera.getParameters().getSupportedWhiteBalance();

		if (mBalancesList == null) {
			mRoot.findViewById(R.id.settingsRowWhiteBalance).setVisibility(View.GONE);
			return;
		}

		initBaseSpinner(spinner, mBalancesList.toArray(new String[mBalancesList.size()]));

		for (int i = 0; i < mBalancesList.size(); i++) {
			if (curBalance.equals(mBalancesList.get(i))) {
				spinner.setSelection(i);
				break;
			}
		}
	}

	private void initSpinnerFlash(Spinner spinner) {
		String curMode = mCamera.getParameters().getFlashMode();
		mFlashesList = mCamera.getParameters().getSupportedFlashModes();

		if (mFlashesList == null) {
			mRoot.findViewById(R.id.settingsFlash).setVisibility(View.GONE);
			return;
		}

		initBaseSpinner(spinner, mFlashesList.toArray(new String[mFlashesList.size()]));

		for (int i = 0; i < mFlashesList.size(); i++) {
			if (curMode.equals(mFlashesList.get(i))) {
				spinner.setSelection(i);
				break;
			}
		}
	}

	private void initSpinnerRecordMove(Spinner spinner) {
		initBaseSpinner(spinner, R.array.settingVideoRecordMode);

		for (int i = 0; i < mRecordMode.length; i++) {
			if (mRecordMode[i] == mSettings.getRecordMode()) {
				spinner.setSelection(i);
				break;
			}
		}
	}

	private void initSpinnerImageHandler(Spinner spinner) {
		initBaseSpinner(spinner, R.array.settingImageHandler);

		for (int i = 0; i < mImageHandler.length; i++) {
			if (mImageHandler[i] == mSettings.getImageHandler()) {
				spinner.setSelection(i);
				break;
			}
		}
	}

	private void initBaseSpinner(Spinner spinner, int resourceId) {
		initBaseSpinner(spinner, mContext.getResources().getStringArray(resourceId));
	}

	private void initBaseSpinner(Spinner spinner, String variants[]) {
		ArrayAdapter adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, variants);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(this);
	}

	public SettingsDialog setOnSettingsChanged(OnSettingsChanged listener) {
		Log.i(TAG, "setOnSettingsChanged: ");
		mOnSettingsChanged = listener;
		return this;
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int i) {
		Log.i(TAG, "onClick: dialog closed");
		mSettings.setDelay(getIntegerValue(editTextDelay, 3000));
		mSettings.setInterval(getIntegerValue(editTextInterval, 5000));
		mSettings.setFPS(getIntegerValue(editTextFPS, 15));
		mSettings.setQuality(seekQuality.getProgress());

		String newPath = editTextPath.getText().toString();
		if (newPath.length() >= 4 && !newPath.equals(mSettings.getPath())) {
			mSettings.setPath(newPath);
			if (mOnSettingsChanged != null) {
				mOnSettingsChanged.onDirectoryChanged(newPath);
			}
		}
		mSettings.save();

		if (mOnSettingsChanged != null) {
			mOnSettingsChanged.onVideoPreferencesChanged();
			mOnSettingsChanged.onImagePreferencesChanged();
		}
	}

	@Override
	public boolean onKey(View view, int i, KeyEvent keyEvent) {
		switch (view.getId()) {
			case R.id.editTextFPS:
				seekFPS.setProgress(getIntegerValue((EditText) view, 15));
				break;
		}
		return true;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
		switch (seekBar.getId()) {
			case R.id.editSeekFPS:
				editTextFPS.setText(String.valueOf(seekBar.getProgress() + 15));
				break;
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
		Log.i(TAG, "onItemSelected: " + adapterView.getId() + " == " + R.id.spinnerFlash);
		switch (adapterView.getId()) {
			case R.id.spinnerFilter:
				mSettings.setEffect(mEffectsList.get(position));
				mCamera.getParameters().setColorEffect(mSettings.getEffect());

				if (mOnSettingsChanged != null) {
					mOnSettingsChanged.onImagePreferencesChanged();
				}
				break;

			case R.id.spinnerSize:
				Log.i(TAG, "onItemSelected: spinnerSize");
				Camera.Size targetSize = mSizesList.get(position);
				Camera.Size cur = mCamera.getParameters().getPictureSize();

				mSettings.setWidth(targetSize.width);
				mSettings.setHeight(targetSize.height);

				if ((targetSize.width != cur.width || targetSize.height != cur.height) && mOnSettingsChanged != null) {
					mOnSettingsChanged.onSizeChanged(targetSize.width, targetSize.height);
				}
				break;

			case R.id.spinnerWhiteBalance:
				mSettings.setBalance(mBalancesList.get(position));
				mCamera.getParameters().setWhiteBalance(mSettings.getBalance());

				if (mOnSettingsChanged != null) {
					mOnSettingsChanged.onImagePreferencesChanged();
				}
				break;

			case R.id.spinnerFlash:
				mSettings.setFlashMode(mFlashesList.get(position));
				mCamera.getParameters().setFlashMode(mSettings.getFlashMode());

				if (mOnSettingsChanged != null) {
					mOnSettingsChanged.onImagePreferencesChanged();
				}
				break;

			case R.id.spinnerMode:
				mSettings.setRecordMode(mRecordMode[position]);
				break;

			case R.id.spinnerHandler:
				mSettings.setImageHandler(mImageHandler[position]);
				break;
		}
	}

	private void fireChange() {
		if (mOnSettingsChanged != null) {
			mOnSettingsChanged.onVideoPreferencesChanged();
			mOnSettingsChanged.onImagePreferencesChanged();
		}
	}

	@Override
	public void onCancel(DialogInterface dialogInterface) {
		Log.i(TAG, "onCancel: ");
		fireChange();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {}
}
