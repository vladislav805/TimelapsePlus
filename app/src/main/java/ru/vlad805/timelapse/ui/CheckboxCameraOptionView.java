package ru.vlad805.timelapse.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.StringRes;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import java.util.List;

/**
 * vlad805 (c) 2018
 */
@SuppressLint("ViewConstructor")
public class CheckboxCameraOptionView extends CameraOptionView<Boolean, Boolean, CheckBox> {

	public CheckboxCameraOptionView(Context context, @StringRes int resId, Boolean data) {
		super(context, resId);

		setControlView(new CheckBox(context));

		setup(data);
	}

	@Override
	public void setup(Boolean data) {
		mControl.setChecked(data);
	}

	@Override
	public Boolean getResult() {
		return mControl.isChecked();
	}

	@Override
	public String toString() {
		return String.valueOf(mControl.isChecked());
	}
}
