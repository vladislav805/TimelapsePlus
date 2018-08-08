package ru.vlad805.timelapse.ui;

import android.content.Context;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.vlad805.timelapse.R;

/**
 * vlad805 (c) 2018
 */
public abstract class CameraOptionView<I, O, V extends View> extends LinearLayout implements ICameraOptionView {

	private TextView mTitle;
	private FrameLayout mWrap;
	V mControl;
	private Validator<O> mValidator;

	public interface Validator<O> {
		boolean validate(O data);
	}

	public CameraOptionView(Context context, @StringRes int title) {
		super(context);
		init(context);
		setTitle(title);
	}

	private void init(Context context) {
		setOrientation(VERTICAL);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		inflater.inflate(R.layout.view_camera_option, this, true);

		mTitle = findViewById(R.id.view_camera_option_title);
		mWrap = findViewById(R.id.view_camera_option_wrap);
	}

	public final ICameraOptionView setTitle(@StringRes int resId) {
		mTitle.setText(resId);
		return this;
	}

	public final ICameraOptionView setValidator(Validator<O> validator) {
		mValidator = validator;
		return this;
	}

	public final boolean validate() {
		return mValidator == null || mValidator.validate(getResult());
	}

	final void setControlView(V v) {
		if (mControl == null) {
			mWrap.addView(mControl = v);
		}
	}

	public V getControl() {
		return mControl;
	}

	protected void setup(I data) {}

	public abstract O getResult();
}
