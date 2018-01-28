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

/**
 * vlad805 (c) 2018
 */
@SuppressWarnings("deprecation")
public class SettingsDialog implements SeekBar.OnSeekBarChangeListener, DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener, DialogInterface.OnCancelListener, View.OnKeyListener {

	private static final String TAG = "Settings";
	private Context mContext;
	private SettingsBundle mSettings;
	private Camera mCamera;

	private EditText editTextDelay;
	private EditText editTextInterval;
	private EditText editTextFPS;
	private SeekBar seekFPS;
	private SeekBar seekQuality;
	private EditText editTextPath;

	private List<String> mEffectsList;

	private List<String> mBalancesList;

	private List<Camera.Size> mSizesList;

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
		View layout = ((LayoutInflater) mContext.getSystemService("layout_inflater")).inflate(R.layout.activity_settings, null);

		TabHost tabHost = layout.findViewById(R.id.settingsTabsHost);
		tabHost.setup();

		tabHost.addTab(tabHost.newTabSpec("image").setContent(R.id.settingsTabImage).setIndicator(mContext.getString(R.string.settingTabImage)));
		tabHost.addTab(tabHost.newTabSpec("video").setContent(R.id.settingsTabVideo).setIndicator(mContext.getString(R.string.settingTabVideo)));
		tabHost.addTab(tabHost.newTabSpec("about").setContent(R.id.settingsTabAbout).setIndicator(mContext.getString(R.string.settingTabAbout)));

		editTextDelay = layout.findViewById(R.id.editTextDelay);
		editTextDelay.setText(String.valueOf(mSettings.getDelay()));

		editTextInterval = layout.findViewById(R.id.editTextInterval);
		editTextInterval.setText(String.valueOf(mSettings.getInterval()));

		editTextFPS = layout.findViewById(R.id.editTextFPS);
		editTextFPS.setText(String.valueOf(mSettings.getFPS()));
		editTextFPS.setOnKeyListener(this);

		seekFPS = layout.findViewById(R.id.editSeekFPS);
		seekFPS.setProgress(mSettings.getFPS() - 15);
		seekFPS.setMax(45);
		seekFPS.setOnSeekBarChangeListener(this);

		seekQuality = layout.findViewById(R.id.editTextQuality);
		seekQuality.setProgress(mSettings.getQuality());
		seekQuality.setMax(100);

		editTextPath = layout.findViewById(R.id.editTextPath);
		editTextPath.setText(mSettings.getPath());

		((TextView) layout.findViewById(R.id.aboutVersion)).setText(String.format(mContext.getString(R.string.aboutVersion), BuildConfig.VERSION_NAME));

		initSpinnerFilter((Spinner) layout.findViewById(R.id.spinnerFilter));
		initSpinnerSize((Spinner) layout.findViewById(R.id.spinnerSize));
		initSpinnerWhiteBalance((Spinner) layout.findViewById(R.id.spinnerWhiteBalance));

		new Builder(mContext).setView(layout).setPositiveButton(R.string.settingsSave, this).setOnCancelListener(this).create().show();
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
			return;
		}

		ArrayAdapter mEffects = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, mEffectsList.toArray(new String[mEffectsList.size()]));
		mEffects.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(mEffects);

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

			ArrayAdapter mSizes = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, sizeArray);
			mSizes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(mSizes);
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

		if (mBalancesList != null) {
			ArrayAdapter mBalances = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, mBalancesList.toArray(new String[mBalancesList.size()]));
			mBalances.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(mBalances);
			for (int i = 0; i < mBalancesList.size(); i++) {
				if (curBalance.equals(mBalancesList.get(i))) {
					spinner.setSelection(i);
					break;
				}
			}
			spinner.setOnItemSelectedListener(this);
		}
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

		//Camera.Size size = mSizesList.get(spinnerSize.getSelectedItemPosition());

		//mSettings.setWidth(size.width);

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
