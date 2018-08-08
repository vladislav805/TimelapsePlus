package ru.vlad805.timelapse.ui;

import android.support.annotation.StringRes;

/**
 * vlad805 (c) 2018
 */
public interface ICameraOptionView {

	public ICameraOptionView setTitle(@StringRes int titleResId);

	public boolean validate();

}
