package ru.vlad805.timelapse;

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

	public PreferenceBundle toBundle() throws IllegalOptionValue {
		PreferenceBundle data = new PreferenceBundle();
		for (Pair<Integer, ICameraOptionView> item : mItems) {
			if (item.first < 0) {
				continue;
			}

			String k = String.valueOf(item.first);
			ICameraOptionView v = item.second;

			if (!v.validate()) {
				throw new IllegalOptionValue(item.first);
			}

			CheckboxCameraOptionView cv = null;
			InputNumberCameraOptionView inv = null;
			InputTextCameraOptionView itv = null;
			SelectCameraOptionView sv = null;
			InputColorCameraOptionView icv = null;
			String str;

			if (v instanceof CheckboxCameraOptionView) {
				cv = (CheckboxCameraOptionView) v;
			} else if (v instanceof InputNumberCameraOptionView) {
				inv = (InputNumberCameraOptionView) v;
			} else if (v instanceof InputTextCameraOptionView) {
				itv = (InputTextCameraOptionView) v;
			} else if (v instanceof SelectCameraOptionView) {
				sv = (SelectCameraOptionView) v;
			} else if (v instanceof InputColorCameraOptionView) {
				icv = (InputColorCameraOptionView) v;
			}

			switch (item.first) {
				case Const.OPTION_RECORD_TYPE:
					assert sv != null;
					data.setRecordType(sv.getResultIndex());
					break;

				case Const.OPTION_VIDEO_RESOLUTION:
					assert sv != null;
					String[] ress = sv.getResult().split("x");
					data.setResolution(Integer.valueOf(ress[0]), Integer.valueOf(ress[1]));
					break;

				case Const.OPTION_VIDEO_FPS:
					assert inv != null;
					data.setFps(inv.getResult());
					break;

				case Const.OPTION_QUALITY:
					assert inv != null;
					data.setQuality(inv.getResult());
					break;

				case Const.OPTION_CAPTURE_DELAY:
					assert inv != null;
					data.setDelay(inv.getResult());
					break;

				case Const.OPTION_CAPTURE_INTERVAL:
					assert inv != null;
					data.setInterval(inv.getResult());
					break;

				case Const.OPTION_RECORD_PATH:
					assert itv != null;
					data.setPath(itv.getResult());
					break;

				case Const.OPTION_CAPTURE_FLASH:
					assert sv != null;
					data.setFlashMode(sv.getResult());
					break;

				case Const.OPTION_PROCESSING_HANDLERS + Const.PROCESSING_HANDLER_DATETIME:
					assert cv != null;
					if (cv.getResult()) {
						data.addProcessingHandler(Const.PROCESSING_HANDLER_DATETIME);
					}
					break;

				case Const.OPTION_PH_DT_ALIGN:
					assert sv != null;
					data.setDateTimeAlign(sv.getResultIndex());
					break;

				case Const.OPTION_PH_DT_TEXT_COLOR:
				case Const.OPTION_PH_DT_BACK_COLOR:
					assert icv != null;
					if (item.first == Const.OPTION_PH_DT_TEXT_COLOR) {
						data.setDateTimeColorText(icv.getResult());
					} else {
						data.setDateTimeColorBack(icv.getResult());
					}
					break;

				case Const.OPTION_PH_DT_TEXT_SIZE:
					assert inv != null;
					data.setDateTimeTextSize(inv.getResult());
					break;

				case Const.OPTION_PROCESSING_HANDLERS + Const.PROCESSING_HANDLER_ALIGN:
					assert cv != null;
					if (cv.getResult()) {
						data.addProcessingHandler(Const.PROCESSING_HANDLER_ALIGN);
					}
					break;

				case Const.OPTION_PROCESSING_HANDLERS + Const.PROCESSING_HANDLER_GEOTRACK:
					assert cv != null;
					if (cv.getResult()) {
						data.addProcessingHandler(Const.PROCESSING_HANDLER_GEOTRACK);
					}
					break;

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
