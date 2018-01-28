package ru.vlad805.timelapse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryReceiver extends BroadcastReceiver {

	public interface OnBatteryLevelChangedListener {
		public void onBatteryLevelChanged(int level);
	}

	private OnBatteryLevelChangedListener mListener;

	public BatteryReceiver(OnBatteryLevelChangedListener listener) {
		mListener = listener;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("BatteryRec1", "onReceive: here");
		Log.i("BatteryRec2", "onReceive: " + (mListener != null));
		Log.i("BatteryRec3", "onReceive: " + intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
		if (mListener != null) {
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

			mListener.onBatteryLevelChanged(
					level != -1 && scale != -1
							? (int) ((level / (float) scale) * 100)
							: -1
			);
		}
	}
}
