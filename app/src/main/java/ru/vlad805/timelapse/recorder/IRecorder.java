package ru.vlad805.timelapse.recorder;

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IRecorder {

	public void stop();
	public void addFrame(byte[] data);
	public int getFrameCount();
	public long getFileSize();

}
