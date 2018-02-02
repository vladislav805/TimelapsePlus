package ru.vlad805.timelapse;

abstract public class Setting {
	public static final String NAME = "TimeLapse";

	public static final String INTRO = "intro";

	public static final String EFFECT = "colorEffect";
	public static final String WHITE_BALANCE = "whiteBalance";
	public static final String DELAY = "delay";
	public static final String INTERVAL = "interval";
	public static final String QUALITY = "quality";
	public static final String FLASH_MODE = "flashMode";

	public static final String FPS = "fps";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	public static final String WORK_DIRECTORY = "workDirectoryPath";

	public static final String ZOOM = "zoom";

	public static final String RECORD_MODE = "recordMode";
	public static final String HANDLER = "imageHandler";
	public static final String REMOTE_CONTROL = "remoteControl";

	public static final String SIZE = "size";

	public static final int INTRO_ABOUT_PLAYING = 0x1;
	public static final int TLIF_VERSION = 1;

	public class RecordMode {
		public static final int VIDEO = 0x1;
		public static final int PHOTO_DIR = 0x2;
	}

	public class ImageHandler {
		public static final int NONE = 0;
		public static final int INSERT_DATE_AND_TIME = 1;
	}
}
