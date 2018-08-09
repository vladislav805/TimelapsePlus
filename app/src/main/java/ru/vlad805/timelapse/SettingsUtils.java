package ru.vlad805.timelapse;

import android.app.Activity;
import android.os.Bundle;
import android.util.Pair;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.AdapterView;
import ru.vlad805.timelapse.ui.*;

import java.util.ArrayList;
import java.util.List;

/**
 * vlad805 (c) 2018
 */
public class SettingsUtils {

	private List<Pair<Integer, ICameraOptionView>> mItems;
	private SparseIntArray mAssoc;
	private OnValueChanged mListener;

	public interface OnValueChanged {
		void onChange(int what);
	}

	public SettingsUtils() {
		mItems = new ArrayList<>();
		mAssoc = new SparseIntArray();
	}

	public SettingsUtils add(int code, ICameraOptionView item) {
		mAssoc.append(code, mItems.size());
		mItems.add(new Pair<>(code, item));

		if (item instanceof CheckboxCameraOptionView) {
			((CheckboxCameraOptionView) item).getControl().setOnCheckedChangeListener((compoundButton, b) -> valueChanged(code));
		} else if (item instanceof InputNumberCameraOptionView || item instanceof InputTextCameraOptionView) {
			//((CameraOptionView) item).getControl().setOn
		} else if (item instanceof SelectCameraOptionView) {
			((SelectCameraOptionView) item).getControl().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					valueChanged(code);
				}

				@Override public void onNothingSelected(AdapterView<?> parent) {}
			});
		}

		return this;
	}

	public <T> T get(int code) {
		int index = mAssoc.get(code, -1);

		if (index == -1) {
			return null;
		}

		return (T) mItems.get(index).second;
	}

	public void toggle(int code, boolean show) {
		View v = get(code);
		v.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	@SuppressWarnings("UnusedReturnValue")
	public SettingsUtils setOnChangeListener(OnValueChanged listener) {
		mListener = listener;
		return this;
	}

	public Bundle toBundle() throws IllegalOptionValue {
		Bundle data = new Bundle();
		for (Pair<Integer, ICameraOptionView> item : mItems) {
			if (item.first < 0) {
				continue;
			}

			String k = String.valueOf(item.first);
			ICameraOptionView v = item.second;

			if (!v.validate()) {
				throw new IllegalOptionValue(item.first);
			}

			if (v instanceof CheckboxCameraOptionView) {
				data.putBoolean(k, ((CheckboxCameraOptionView) v).getResult());
			} else if (v instanceof InputNumberCameraOptionView) {
				data.putInt(k, ((InputNumberCameraOptionView) v).getResult());
			} else if (v instanceof InputTextCameraOptionView) {
				data.putString(k, ((InputTextCameraOptionView) v).getResult());
			} else if (v instanceof SelectCameraOptionView) {
				data.putString(k, ((SelectCameraOptionView) v).getResult());
			} else if (v instanceof InputColorCameraOptionView) {
				data.putInt(k, ((InputColorCameraOptionView) v).getResult());
			}

		}
		return data;
	}

	public View[] getViews() {
		View[] views = new View[mItems.size()];

		int i = 0;
		for (Pair<Integer, ICameraOptionView> item : mItems) {
			views[i++] = (View) item.second;
			valueChanged(item.first);
		}
		return views;
	}

	private void valueChanged(int code) {
		if (mListener != null) {
			mListener.onChange(code);
		}
	}


}
