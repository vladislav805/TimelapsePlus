package ru.vlad805.timelapse;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	private static final int CHECK_PERMISSIONS = 0x100;

	private static final String[] permissions = new String[] {
			Manifest.permission.INTERNET,
			Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.WAKE_LOCK
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_holder);

		if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
			if (checkPermissions()) {
				ok();
			} else {
				Toast.makeText(this, "Please, give all permissions...", Toast.LENGTH_LONG).show();
			}
		} else {
			ok();
		}
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == CHECK_PERMISSIONS) {
			int ok = 0;
			for (int item : grantResults) {
				if (item == PackageManager.PERMISSION_GRANTED) {
					ok++;
				}
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
				listPermissionsNeeded.add(p);
			}
		}

		if (!listPermissionsNeeded.isEmpty()) {
			ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
			return false;
		}
		return true;
	}

	private void ok() {
		startActivity(new Intent(this, TimeLapseActivity.class));
		finish();
	}
}
