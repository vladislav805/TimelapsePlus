package ru.vlad805.timelapse;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

	private static final int CHECK_PERMISSIONS = 0x100;

	private static final String[] permissions = new String[] {
			Manifest.permission.INTERNET,
			//Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.WAKE_LOCK
	};
	private static final String TAG = "TimeLapse";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_holder);

		Log.e(TAG, "access camera: " + (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) );

		if (checkPermissions()) {
			//if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
				ok();
			/*} else {
				Toast.makeText(this, "Please, give all permissions...", Toast.LENGTH_LONG).show();
			}*/
		} else {
			Toast.makeText(this, "Please, give all permissions...", Toast.LENGTH_LONG).show();
		}
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == CHECK_PERMISSIONS) {
			int ok = 0;
			int i = 0;
			for (int item : grantResults) {
				Log.d(TAG, permissions[i] + " = " + grantResults[i]);
				if (item == PackageManager.PERMISSION_GRANTED) {
					ok++;
				}
				i++;
			}

			if (ok == grantResults.length) {
				ok();
			} else {
				Toast.makeText(this, R.string.errorPermission, Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}

	private boolean checkPermissions() {
		List<String> listPermissionsNeeded = new ArrayList<>();

		for (String p : permissions) {
			int result = ContextCompat.checkSelfPermission(this, p);
			if (result != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.shouldShowRequestPermissionRationale(this, p);
				listPermissionsNeeded.add(p);
			}
		}

		if (!listPermissionsNeeded.isEmpty()) {
			Log.d(TAG, array2string(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()])));
			ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), CHECK_PERMISSIONS);
			return false;
		}
		return true;
	}

	private void ok() {
		startActivity(new Intent(this, TimeLapseActivity.class));
		finish();
	}

	private static String array2string(String[] s) {
		StringBuilder sb = new StringBuilder();
		for (String i : s) {
			sb.append(i).append("; ");
		}
		return sb.toString();
	}
}
