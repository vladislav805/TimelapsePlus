package ru.vlad805.timelapse.imagehandler;

import android.graphics.*;
import ru.vlad805.timelapse.Setting;
import ru.vlad805.timelapse.SettingsBundle;

import java.io.ByteArrayOutputStream;
import java.lang.ref.SoftReference;
import java.util.Calendar;
import java.util.Locale;

public class DateTimeImageHandler implements IImageHandler {

	private SoftReference<SettingsBundle> mSettings;
	private Bitmap mBitmap;

	public DateTimeImageHandler(SettingsBundle settings) {
		mSettings = new SoftReference<>(settings);
	}

	@Override
	public int getId() {
		return Setting.ImageHandler.INSERT_DATE_AND_TIME;
	}

	@Override
	public DateTimeImageHandler handle(byte[] data) {
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inMutable = true;
		mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opt);
		Canvas canvas = new Canvas(mBitmap);

		String text = getDateTimeString();

		int margin = mBitmap.getHeight() / 22;
		int fontSize = mBitmap.getHeight() / 20;

		int x = margin + 1;
		int y = mBitmap.getHeight() - margin;

		int padding = fontSize / 3;

		Rect size = new Rect();
		Paint backgroundColor = new Paint();
		backgroundColor.setColor(Color.argb(100, 0, 0, 0));
		backgroundColor.setTextSize(fontSize);
		backgroundColor.getTextBounds(text, 0, text.length(), size);

		canvas.drawRect(
				x - padding,
				y - size.height() - padding,
				x + size.width() + padding,
				y + padding,
				backgroundColor
		);

		Paint textColor = new Paint();
		textColor.setColor(Color.WHITE);
		textColor.setTextSize(fontSize);

		canvas.drawText(text, x, y, textColor);

		return this;
	}

	private String getDateTimeString() {
		Calendar c = Calendar.getInstance();
		return String.format(Locale.getDefault(),
				"%02d/%02d/%04d %02d:%02d:%02d",
				c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND)
		);
	}

	public byte[] getBytes() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		int quality = 90;

		if (mSettings.get() != null) {
			quality = mSettings.get().getQuality();
		}

		mBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
		return stream.toByteArray();
	}

	@Override
	public void destroy() {
		mBitmap = null;
		mSettings.clear();
	}

}
