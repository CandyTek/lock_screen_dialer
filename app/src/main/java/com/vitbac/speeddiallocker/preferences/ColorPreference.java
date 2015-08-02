package com.vitbac.speeddiallocker.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vitbac.speeddiallocker.R;


/**
 * Created by nick on 7/1/15.
 */
public class ColorPreference extends Preference {

    private final static String TAG = "ColorPreference";
    private int mColor;
    private View mColorView;

    public ColorPreference(Context context) {
        super(context);
    }

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressWarnings("NewApi")
    public ColorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.view_color_pref, parent, false);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        final View colorView = view.findViewById(R.id.preference_color_view);
        if (colorView != null) {
            //Log.d(TAG, "Setting view color in ColorPreference " + mColor);
            colorView.setBackgroundColor(mColor);
            mColorView = colorView;

        } else {
            colorView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            try {
                mColor = getPersistedInt(getContext().getResources().getColor(R.color.green));
            } catch (ClassCastException e) {
                Log.e(TAG, "Could not cast type int to default value");
            }
        } else if (defaultValue != null) {
            mColor = (int) defaultValue;
            persistInt(mColor);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, getContext().getResources().getColor(R.color.green));
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        if (isPersistent()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.value = mColor;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mColor = myState.value;
    }

    public void setColor(int color) {
        mColor = color;
        persistInt(mColor);
        if (mColorView != null) {
            mColorView.setBackgroundColor(mColor);
        }
    }

    private static class SavedState extends BaseSavedState {
        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        // Member that holds the setting's value
        // Change this data type to match the type saved by your Preference
        int value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readInt();  // Change this to read the appropriate data type
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeInt(value);  // Change this to write the appropriate data type
        }
    }
}
