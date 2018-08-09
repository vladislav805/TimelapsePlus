package ru.vlad805.timelapse;

/**
 * vlad805 (c) 2018
 */
public class IllegalOptionValue extends Exception {

	private int mWhat;

	public IllegalOptionValue(int what) {
		mWhat = what;
	}

	public int getWhat() {
		return mWhat;
	}

	@Override
	public String getMessage() {
		return "invalid value on " + mWhat;
	}
}
