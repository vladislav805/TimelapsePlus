package ru.vlad805.timelapse.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.StringRes;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.List;

/**
 * vlad805 (c) 2018
 */
@SuppressLint("ViewConstructor")
public class SelectCameraOptionView extends CameraOptionView<List<String>, String, Spinner> {

	public SelectCameraOptionView(Context context, @StringRes int resId, List<String> data) {
		super(context, resId);

		setControlView(new Spinner(context, Spinner.MODE_DIALOG));
		setup(data);
	}

	@Override
	protected void setup(List<String> data) {
		ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, data);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mControl.setAdapter(adapter);
	}

	@Override
	public String getResult() {
		return mControl.getSelectedItem().toString();
	}

	public int getResultIndex() {
		return mControl.getSelectedItemPosition();
	}

	@Override
	public String toString() {
		return getResult();
	}
}
