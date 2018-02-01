package ru.vlad805.timelapse;

abstract public class Setting {
	public static final String NAME = "TimeLapse";

	public static final String INTRO = "INTRO";

	public static final String EFFECT = "COLOR_EFFECT";
	public static final String DELAY = "DELAY";
	public static final String FPS = "FPS";
	public static final String HEIGHT = "HEIGHT";
	public static final String INTERVAL = "INTERVAL";
	public static final String QUALITY = "QUALITY";
	public static final String WHITE_BALANCE = "WHITE_BALANCE";
	public static final String WIDTH = "WIDTH";
	public static final String ZOOM = "ZOOM";
	public static final String WORK_DIRECTORY = "PATH";
	public static final String FLASH_MODE = "FLASH_MODE";
	public static final String RECORD_MODE = "RECORD_MODE";
	public static final String HANDLER = "HANDLER";
	public static final String REMOTE_CONTROL = "REMOTE_CONTROL";
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
