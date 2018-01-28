package ru.vlad805.timelapse;

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IImageHandler {

	public int getId();
	public IImageHandler handle(byte[] data);
	public byte[] getBytes();
	public void destroy();

}
