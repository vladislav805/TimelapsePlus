package ru.vlad805.timelapse.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.StringRes;
import android.support.v4.graphics.ColorUtils;
import android.view.View;
import android.widget.Button;
import yuku.ambilwarna.AmbilWarnaDialog;

/**
 * vlad805 (c) 2018
 */
@SuppressLint("ViewConstructor")
public class InputColorCameraOptionView extends CameraOptionView<Integer, Integer, Button> implements View.OnClickListener, AmbilWarnaDialog.OnAmbilWarnaListener {

	private AmbilWarnaDialog mColorPicker;

	private int mColor;

	public InputColorCameraOptionView(Context context, @StringRes int title, Integer data) {
		super(context, title);

		setControlView(new Button(context));
		mColorPicker = new AmbilWarnaDialog(context, data, true, this);

		mColor = data;
		mControl.setOnClickListener(this);
		setup(data);
	}

	@Override
	protected void setup(Integer data) {
		mControl.setText(String.format("#%08X", (data)));
		mControl.setBackgroundColor(data);
		mControl.setTextColor(isDark(data) ? Color.WHITE : Color.BLACK);
	}

	@Override
	public Integer getResult() {
		return mColor;
	}

	@Override
	public void onClick(View view) {
		if (view.equals(mControl)) {
			mColorPicker.show();
		}
	}

	private boolean isDark(int color) {
		return ColorUtils.calculateLuminance(color) < 0.5;
	}

	@Override
	public void onOk(AmbilWarnaDialog dialog, int color) {
		mColor = color;
		setup(color);
	}

	@Override
	public void onCancel(AmbilWarnaDialog dialog) {

	}
}
