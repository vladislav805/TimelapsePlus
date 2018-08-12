package ru.vlad805.timelapse.handler;

import android.graphics.*;

import android.util.SparseLongArray;
import ru.vlad805.timelapse.PreferenceBundle;

import java.util.Calendar;
import java.util.Locale;

/**
 * vlad805 (c) 2018
 */
public class DateTimeImageHandler extends ImageHandler {

	private Calendar mCalendar = Calendar.getInstance();
	private SparseLongArray mData;

	public DateTimeImageHandler(PreferenceBundle bundle) {
		super(bundle);
	}

	@Override
	public void onCaptureStart() {
		mData = new SparseLongArray();
	}

	@Override
	public void onImageCaptured(int index) {
		mData.put(index, System.currentTimeMillis());
	}

	@Override
	public void onCaptureStop() {

	}

	@Override
	public Bitmap processImage(Bitmap image, int index) {
		Canvas canvas = new Canvas(image);
		String text = getDateTimeString(mData.get(index));

		int margin = image.getHeight() / 22;
		int fontSize = image.getHeight() / 100 * getPreference().getDateTimeTextSize();

		int x, y;

		int padding = fontSize / 3;

		Rect size = new Rect();
		Paint backgroundColor = new Paint();

		backgroundColor.setColor(getPreference().getDateTimeColorBack());
		backgroundColor.setTextSize(fontSize);
		backgroundColor.getTextBounds(text, 0, text.length(), size);

		switch (getPreference().getDateTimeAlign()) {
			case 0: // Bottom Left
				x = margin;
				y = image.getHeight() - margin;
				break;

			case 1: // Bottom Right
				x = image.getWidth() - margin - size.width();
				y = image.getHeight() - margin;
				break;

			case 2: // Top Left
				x = margin;
				y = margin;
				break;

			case 3: // Top Right
				x = image.getWidth() - margin - size.width();
				y = margin;
				break;

			default:
				throw new IllegalArgumentException("Invalid align");
		}


		canvas.drawRect(
				x - padding,
				y - size.height() - padding,
				x + size.width() + padding,
				y + padding,
				backgroundColor
		);

		Paint textColor = new Paint();
		textColor.setColor(getPreference().getDateTimeColorText());
		textColor.setTextSize(fontSize);

		canvas.drawText(text, x, y, textColor);

		return image;
	}

	private String getDateTimeString(long date) {
		mCalendar.setTimeInMillis(date);
		return String.format(Locale.getDefault(),
				"%02d/%02d/%04d %02d:%02d:%02d",
				mCalendar.get(Calendar.DAY_OF_MONTH),
				mCalendar.get(Calendar.MONTH) + 1,
				mCalendar.get(Calendar.YEAR),
				mCalendar.get(Calendar.HOUR_OF_DAY),
				mCalendar.get(Calendar.MINUTE),
				mCalendar.get(Calendar.SECOND)
		);
	}
}
