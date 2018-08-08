package ru.vlad805.timelapse.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.StringRes;
import android.text.InputType;
import android.widget.EditText;

/**
 * vlad805 (c) 2018
 */
@SuppressLint("ViewConstructor")
public class InputTextCameraOptionView extends CameraOptionView<String, String, EditText> {

	public InputTextCameraOptionView(Context context, @StringRes int title, String data) {
		super(context, title);

		setControlView(new EditText(context));
		mControl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

		setup(data);
	}

	@Override
	protected void setup(String data) {
		mControl.setText(data);
	}

	@Override
	public String getResult() {
		return mControl.getText().toString();
	}

}
