package ru.vlad805.timelapse.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import ru.vlad805.timelapse.R;

public class TestActivity extends AppCompatActivity {

	private static final String TAG = TestActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);

		Bundle b = getIntent().getExtras();

		if (b == null) {
			return;
		}

		for (String key : b.keySet()) {
			Log.i(TAG, "onCreate: " + key + " = " + String.valueOf(b.get(key)));
		}
	}
}
