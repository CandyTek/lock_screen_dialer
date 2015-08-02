/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vitbac.speeddiallocker.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.NumberPicker;
import com.vitbac.speeddiallocker.R;

/*
 * @author Danesh
 * @author nebkat
 */

public class TimerPreference extends DialogPreference {
    private int mMinSeconds, mMaxSeconds, mDefaultSeconds;
    private int mMinMinutes, mMaxMinutes, mDefaultMinutes;

    private String mMaxExternalKeyMinute, mMinExternalKeyMinute;
    private String mMaxExternalKeySecond, mMinExternalKeySecond;

    private String mPickerTitleSeconds;
    private String mPickerTitleMinutes;

    private NumberPicker mNumberPickerSeconds;
    private NumberPicker mNumberPickerMinutes;

    public TimerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        /*TypedArray dialogType = context.obtainStyledAttributes(attrs,
                Resources.getSystem().getIdentifier("DialogPreference", "id", "android"), 0, 0);*/
                //com.android.internal.R.styleable.DialogPreference, 0, 0);
        TypedArray timerPrefTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.TimerPreference, 0, 0);

        mMaxExternalKeySecond = timerPrefTypedArray.getString(R.styleable.TimerPreference_maxExternalSeconds);
        mMinExternalKeySecond = timerPrefTypedArray.getString(R.styleable.TimerPreference_minExternalSeconds);
        mMaxExternalKeyMinute = timerPrefTypedArray.getString(R.styleable.TimerPreference_maxExternalMinutes);
        mMinExternalKeyMinute = timerPrefTypedArray.getString(R.styleable.TimerPreference_minExternalMinutes);

        mPickerTitleSeconds = timerPrefTypedArray.getString(R.styleable.TimerPreference_secondsTitle);
        mPickerTitleMinutes = timerPrefTypedArray.getString(R.styleable.TimerPreference_minutesTitle);

        mMaxSeconds = timerPrefTypedArray.getInt(R.styleable.TimerPreference_maxSeconds, 59);
        mMinSeconds = timerPrefTypedArray.getInt(R.styleable.TimerPreference_minSeconds, 0);
        mMaxMinutes = timerPrefTypedArray.getInt(R.styleable.TimerPreference_maxMinutes, 60);
        mMinMinutes = timerPrefTypedArray.getInt(R.styleable.TimerPreference_minMinutes, 0);

        mDefaultSeconds = timerPrefTypedArray.getInt(R.styleable.TimerPreference_defaultValueSeconds, mMinSeconds);
        mDefaultMinutes = timerPrefTypedArray.getInt(R.styleable.TimerPreference_defaultValueMinutes, mMinMinutes);

        //dialogType.recycle();
        timerPrefTypedArray.recycle();
    }

    @Override
    protected View onCreateDialogView() {
        int maxSeconds = mMaxSeconds;
        int minSeconds = mMinSeconds;
        int maxMinutes = mMaxMinutes;
        int minMinutes = mMinMinutes;

        // External values
        if (mMaxExternalKeySecond != null) {
            maxSeconds = getSharedPreferences().getInt(mMaxExternalKeyMinute, mMaxSeconds);
        }
        if (mMinExternalKeySecond != null) {
            minSeconds = getSharedPreferences().getInt(mMinExternalKeyMinute, mMinSeconds);
        }
        if (mMaxExternalKeyMinute != null) {
            maxMinutes = getSharedPreferences().getInt(mMaxExternalKeySecond, mMaxMinutes);
        }
        if (mMinExternalKeyMinute != null) {
            minMinutes = getSharedPreferences().getInt(mMinExternalKeySecond, mMinMinutes);
        }

        LayoutInflater inflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_pref_timer, null);

        mNumberPickerSeconds = (NumberPicker) view.findViewById(R.id.number_picker_seconds);
        mNumberPickerMinutes = (NumberPicker) view.findViewById(R.id.number_picker_minutes);

        if (mNumberPickerSeconds == null || mNumberPickerMinutes == null) {
            throw new RuntimeException("mNumberPickerSeconds or mNumberPickerMinutes is null!");
        }

        // Initialize state
        mNumberPickerSeconds.setMaxValue(maxSeconds);
        mNumberPickerSeconds.setMinValue(minSeconds);
        mNumberPickerSeconds.setValue(getPersistedInt(mDefaultSeconds) % 60);
        //mNumberPickerSeconds.setValue(getPersistedValue(1));
        mNumberPickerSeconds.setWrapSelectorWheel(true);
        mNumberPickerMinutes.setMaxValue(maxMinutes);
        mNumberPickerMinutes.setMinValue(minMinutes);
        mNumberPickerMinutes.setValue(getPersistedInt(mDefaultMinutes) / 60);
        //mNumberPickerMinutes.setValue(getPersistedValue(2));
        mNumberPickerMinutes.setWrapSelectorWheel(false);

        // Titles
        TextView pickerTitleSeconds = (TextView) view.findViewById(R.id.picker_title_seconds);
        TextView pickerTitleMinutes = (TextView) view.findViewById(R.id.picker_title_minutes);

        if (pickerTitleSeconds != null && pickerTitleMinutes != null) {
            pickerTitleSeconds.setText(mPickerTitleSeconds);
            pickerTitleMinutes.setText(mPickerTitleMinutes);
        }

        // No keyboard popup
        EditText textInputSeconds = //(EditText) mNumberPickerSeconds.findViewById(com.android.internal.R.id.numberpicker_input);
                (EditText) mNumberPickerSeconds.findViewById(
                        Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"));
        EditText textInputMinutes = //(EditText) mNumberPickerMinutes.findViewById(com.android.internal.R.id.numberpicker_input);
                (EditText) mNumberPickerMinutes.findViewById(
                        Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"));
        if (textInputSeconds != null && textInputMinutes != null) {
            textInputSeconds.setCursorVisible(false);
            textInputSeconds.setFocusable(false);
            textInputSeconds.setFocusableInTouchMode(false);
            textInputMinutes.setCursorVisible(false);
            textInputMinutes.setFocusable(false);
            textInputMinutes.setFocusableInTouchMode(false);
        }

        return view;
    }

    /*private int getPersistedValue(int value) {
        String[] values = getPersistedString(mDefaultSeconds + "|" + mDefaultMinutes).split("\\|");
        if (value == 1) {
            try {
                return Integer.parseInt(values[0]);
            } catch (NumberFormatException e) {
                return mDefaultSeconds;
            }
        } else {
            try {
                return Integer.parseInt(values[1]);
            } catch (NumberFormatException e) {
                return mDefaultMinutes;
            }
        }
    }*/

   /* @Override
    protected int getPersistedInt(int def) {
        *//*int seconds = getPersistedValue(1);
        int minutes = getPersistedValue(2);
        return seconds + minutes * 60;*//*
        return getPersistedInt(mDefaultSeconds + mDefaultMinutes * 60);
    }
*/
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            //persistString(mNumberPickerSeconds.getValue() + "|" + mNumberPickerMinutes.getValue());
            persistInt(mNumberPickerMinutes.getValue() * 60 + mNumberPickerSeconds.getValue());
        }
    }

    public void setMinSeconds(int min) {
        mMinSeconds = min;
    }
    public void setMaxSeconds(int max) {
        mMaxSeconds = max;
    }
    public void setMinMinutes(int min) {
        mMinMinutes = min;
    }
    public void setMaxMinutes(int max) {
        mMaxMinutes = max;
    }
    public void setDefaultSeconds(int def) {
        mDefaultSeconds = def;
    }
    public void setDefaultMinutes(int def) {
        mDefaultMinutes = def;
    }

}