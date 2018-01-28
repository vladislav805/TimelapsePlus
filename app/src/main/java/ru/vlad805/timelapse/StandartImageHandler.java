package ru.vlad805.timelapse;

public class StandartImageHandler implements IImageHandler {

	private byte[] mData;

	public StandartImageHandler() {

	}

	@Override
	public int getId() {
		return Setting.ImageHandler.NONE;
	}

	@Override
	public IImageHandler handle(byte[] data) {
		mData = data;
		return this;
	}

	@Override
	public byte[] getBytes() {
		return mData;
	}

	@Override
	public void destroy() {
		mData = null;
	}
}
