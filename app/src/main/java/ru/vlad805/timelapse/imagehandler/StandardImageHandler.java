package ru.vlad805.timelapse.imagehandler;

import ru.vlad805.timelapse.Setting;

public class StandardImageHandler implements IImageHandler {

	private byte[] mData;

	public StandardImageHandler() {

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
