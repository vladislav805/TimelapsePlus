package ru.vlad805.timelapse;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntRange;

import java.util.ArrayList;
import java.util.List;

/**
 * vlad805 (c) 2018
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class PreferenceBundle implements Parcelable {

	private Resolution resolution;

	private String flashMode;

	@IntRange(from = 0)
	private int delay;

	@IntRange(from = 50, to = 180000)
	private int interval;

	@IntRange(from = 15, to = 60)
	private int fps;

	@IntRange(from = 0, to = 100)
	private int quality;

	private String path;

	private String filter;

	private String whiteBalance;

	private ArrayList<Integer> processingHandlers;

	private int processingHandlerDateTimeAlign;
	private int processingHandlerDateTimeColorText;
	private int processingHandlerDateTimeColorBack;
	private int processingHandlerDateTimeTextSize;

	@IntRange(from = 0, to = 1)
	private int recordType;

	@SuppressWarnings("unchecked")
	protected PreferenceBundle(Parcel in) {
		int[] tmpRes = new int[2];
		in.readIntArray(tmpRes);
		resolution = new Resolution(tmpRes[0], tmpRes[1]);
		flashMode = in.readString();
		delay = in.readInt();
		interval = in.readInt();
		fps = in.readInt();
		quality = in.readInt();
		path = in.readString();
		filter = in.readString();
		whiteBalance = in.readString();
		processingHandlers = new ArrayList<>();
		in.readList(processingHandlers, Integer.class.getClassLoader());
		processingHandlerDateTimeAlign = in.readInt();
		processingHandlerDateTimeColorText = in.readInt();
		processingHandlerDateTimeColorBack = in.readInt();
		processingHandlerDateTimeTextSize = in.readInt();
		recordType = in.readInt();
	}

	public static final Creator<PreferenceBundle> CREATOR = new Creator<PreferenceBundle>() {
		@Override
		public PreferenceBundle createFromParcel(Parcel in) {
			return new PreferenceBundle(in);
		}

		@Override
		public PreferenceBundle[] newArray(int size) {
			return new PreferenceBundle[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeIntArray(new int[] {resolution.width, resolution.height});
		parcel.writeString(flashMode);
		parcel.writeInt(delay);
		parcel.writeInt(interval);
		parcel.writeInt(fps);
		parcel.writeInt(quality);
		parcel.writeString(path);
		parcel.writeString(filter);
		parcel.writeString(whiteBalance);
		parcel.writeList(processingHandlers);
		parcel.writeInt(processingHandlerDateTimeAlign);
		parcel.writeInt(processingHandlerDateTimeColorText);
		parcel.writeInt(processingHandlerDateTimeColorBack);
		parcel.writeInt(processingHandlerDateTimeTextSize);
		parcel.writeInt(recordType);
	}

	public class Resolution {
		private int width;
		private int height;

		public Resolution(int w, int h) {
			width = w;
			height = h;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		@Override
		public String toString() {
			return width + "x" + height;
		}
	}

	public PreferenceBundle() {
		processingHandlers = new ArrayList<>();
	}

	public void setRecordType(int rt) {
		recordType = rt;
	}

	public int getRecordType() {
		return recordType;
	}

	public void setResolution(int w, int h) {
		setResolution(new Resolution(w, h));
	}

	public void setResolution(Resolution r) {
		resolution = r;
	}

	public Resolution getResolution() {
		return resolution;
	}

	public void setFps(int fps) {
		this.fps = fps;
	}

	public int getFps() {
		return fps;
	}

	public void setQuality(int quality) {
		this.quality = quality;
	}

	public int getQuality() {
		return quality;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public int getDelay() {
		return delay;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public int getInterval() {
		return interval;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setFlashMode(String flashMode) {
		this.flashMode = flashMode;
	}

	public String getFlashMode() {
		return flashMode;
	}

	public void setFilter(String f) {
		filter = f;
	}

	public String getFilter() {
		return filter;
	}

	public void setWhiteBalance(String wb) {
		whiteBalance = wb;
	}

	public String getWhiteBalance() {
		return whiteBalance;
	}

	public void addProcessingHandler(int handler) {
		processingHandlers.add(handler);
	}

	public List<Integer> getProcessingHandlers() {
		return processingHandlers;
	}

	public boolean hasProcessingHandler(int handler) {
		return processingHandlers.contains(handler);
	}

	public void setDateTimeAlign(int align) {
		processingHandlerDateTimeAlign = align;
	}

	public int getDateTimeAlign() {
		return processingHandlerDateTimeAlign;
	}

	public void setDateTimeColorText(int color) {
		processingHandlerDateTimeColorText = color;
	}

	public int getDateTimeColorText() {
		return processingHandlerDateTimeColorText;
	}

	public void setDateTimeColorBack(int color) {
		processingHandlerDateTimeColorBack = color;
	}

	public int getDateTimeColorBack() {
		return processingHandlerDateTimeColorBack;
	}

	public void setDateTimeTextSize(int size) {
		processingHandlerDateTimeTextSize = size;
	}

	public int getDateTimeTextSize() {
		return processingHandlerDateTimeTextSize;
	}
}
