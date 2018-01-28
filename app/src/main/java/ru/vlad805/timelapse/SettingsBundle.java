package ru.vlad805.timelapse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

@SuppressWarnings("UnusedReturnValue")
public class SettingsBundle {

	private Context mContext;

	private int mWidth = 0;
	private int mHeight = 0;
	private int mDelay;
	private int mFPS;
	private int mInterval;

	private String mBalance = "";
	private String mEffect = "";
	private String mFlash = "";
	private String mPath = null;
	private int mQuality;

	private int mIntro;

	public SettingsBundle(Context context) {
		mContext = context;
	}

	public SettingsBundle load() {
		SharedPreferences settings = mContext.getSharedPreferences(Setting.NAME, Context.MODE_PRIVATE);
		mEffect = settings.getString(Setting.EFFECT, "");
		mWidth = settings.getInt(Setting.WIDTH, 0);
		mHeight = settings.getInt(Setting.HEIGHT, 0);
		mBalance = settings.getString(Setting.WHITE_BALANCE, "");
		mDelay = settings.getInt(Setting.DELAY, 3000);
		mInterval = settings.getInt(Setting.INTERVAL, 5000);
		mFPS = settings.getInt(Setting.FPS, 25);
		mQuality = settings.getInt(Setting.QUALITY, 70);
		mFlash = settings.getString(Setting.FLASH_MODE, "");
		mPath = settings.getString(Setting.WORK_DIRECTORY, Environment.getExternalStorageDirectory().getAbsolutePath() + "/TimelapseDir/");
		mIntro = settings.getInt(Setting.INTRO, 0);
		return this;
	}
	
	public SettingsBundle save() {
		SharedPreferences.Editor editor = mContext.getSharedPreferences(Setting.NAME, Context.MODE_PRIVATE).edit();
		editor.putString(Setting.EFFECT, mEffect);
		editor.putInt(Setting.WIDTH, mWidth);
		editor.putInt(Setting.HEIGHT, mHeight);
		editor.putString(Setting.WHITE_BALANCE, mBalance);
		editor.putInt(Setting.DELAY, mDelay);
		editor.putInt(Setting.INTERVAL, mInterval);
		editor.putInt(Setting.FPS, mFPS);
		editor.putInt(Setting.QUALITY, mQuality);
		editor.putString(Setting.FLASH_MODE, mFlash);
		editor.putString(Setting.WORK_DIRECTORY, mPath);
		editor.putInt(Setting.INTRO, mIntro);
		editor.apply();
		return this;
	}

	
	
	public int getDelay() {
		return mDelay;
	}

	public int getFPS() {
		return mFPS;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public int getInterval() {
		return mInterval;
	}

	public int getIntro() {
		return mIntro;
	}

	public int getQuality() {
		return mQuality;
	}

	public String getFlashMode() {
		return mFlash;
	}

	public String getBalance() {
		return mBalance;
	}

	public String getEffect() {
		return mEffect;
	}

	public String getPath() {
		return mPath;
	}

	
	
	public SettingsBundle setBalance(String balance) {
		mBalance = balance;
		return this;
	}
	
	public SettingsBundle setDelay(int delay) {
		mDelay = toRange(delay, 0, Integer.MAX_VALUE);
		return this;
	}

	public SettingsBundle setEffect(String effect) {
		mEffect = effect;
		return this;
	}

	public SettingsBundle setFPS(int fps) {
		mFPS = toRange(fps, 15, 60);
		return this;
	}

	public SettingsBundle setWidth(int width) {
		mWidth = toRange(width, 100, 8000);
		return this;
	}

	public SettingsBundle setHeight(int height) {
		mHeight = toRange(height, 100, 8000);
		return this;
	}

	public SettingsBundle setInterval(int interval) {
		mInterval = toRange(interval, 100, 3600000);
		return this;
	}

	public SettingsBundle setIntro(int intro) {
		mIntro = intro;
		return this;
	}

	public SettingsBundle setPath(String path) {
		mPath = path;
		return this;
	}

	public SettingsBundle setQuality(int quality) {
		mQuality = toRange(quality, 0, 100);
		return this;
	}

	public SettingsBundle setFlashMode(String mode) {
		mFlash = mode;
		return this;
	}

	private int toRange(int value, int min, int max) {
		return Math.min(max, Math.max(min, value));
	}
}
