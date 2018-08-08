package ru.vlad805.timelapse.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.vlad805.timelapse.R;

/**
 * vlad805 (c) 2018
 */
@SuppressLint("ViewConstructor")
public class CameraOptionHeaderView extends LinearLayout implements ICameraOptionView {

	private TextView mTitle;

	public CameraOptionHeaderView(Context context, @StringRes int title) {
		super(context);
		init(context);
		setTitle(title);
	}

	private void init(Context context) {
		setOrientation(VERTICAL);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		inflater.inflate(R.layout.view_camera_option_header, this, true);

		mTitle = findViewById(R.id.view_camera_option_header_title);
	}

	@Override
	public boolean validate() {
		return true;
	}

	@Override
	public final ICameraOptionView setTitle(@StringRes int resId) {
		mTitle.setText(resId);
		return this;
	}

}
