package ru.vlad805.timelapse.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.StringRes;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;

/**
 * vlad805 (c) 2018
 */
@SuppressLint("ViewConstructor")
public class InputNumberCameraOptionView extends CameraOptionView<Integer, Integer, EditText> {

	public InputNumberCameraOptionView(Context context, @StringRes int title, Integer data) {
		super(context, title);

		setControlView(new EditText(context));
		mControl.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

		setup(data);
	}

	@Override
	protected void setup(Integer data) {
		mControl.setText(String.valueOf(data));
	}

	@Override
	public Integer getResult() {
		return Integer.valueOf(mControl.getText().toString());
	}

	@Override
	public String toString() {
		return mControl.getText().toString();
	}
}
