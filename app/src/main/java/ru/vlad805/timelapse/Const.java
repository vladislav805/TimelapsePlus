package ru.vlad805.timelapse;

/**
 * vlad805 (c) 2018
 */
@SuppressWarnings("WeakerAccess")
public final class Const {

	public static final int CATEGORY_VIDEO = -1;
	public static final int CATEGORY_CAPTURING = -2;
	public static final int CATEGORY_CAMERA = -3;
	public static final int CATEGORY_EXTRA = -4;

	public static final int OPTION_VIDEO_RESOLUTION = 1;
	public static final int OPTION_CAPTURE_FLASH = 2;
	public static final int OPTION_CAPTURE_INTERVAL = 3;
	public static final int OPTION_CAPTURE_DELAY = 4;
	public static final int OPTION_VIDEO_FPS = 5;
	public static final int OPTION_QUALITY = 6;
	public static final int OPTION_RECORD_PATH = 7;
	public static final int OPTION_CAPTURE_FILTER = 8;
	public static final int OPTION_CAPUTRE_WHITE_BALANCE = 9;
	public static final int OPTION_RECORD_TYPE = 10;
	public static final int OPTION_PROCESSING_HANDLERS = 11;
	public static final int OPTION____NEXT =  OPTION_PROCESSING_HANDLERS + 3;

	public static final int OPTION_PH_DT_ALIGN = 100;
	public static final int OPTION_PH_DT_TEXT_COLOR = 101;
	public static final int OPTION_PH_DT_BACK_COLOR = 102;
	public static final int OPTION_PH_DT_TEXT_SIZE = 103;

	public static final int RECORD_TYPE_MP4 = 0;
	public static final int RECORD_TYPE_JPG = 1;

	public static final int PROCESSING_HANDLER_DATETIME = 0;
	public static final int PROCESSING_HANDLER_ALIGN = 1;
	public static final int PROCESSING_HANDLER_GEOTRACK = 2;

}
