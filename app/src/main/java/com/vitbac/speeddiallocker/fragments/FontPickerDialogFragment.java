package com.vitbac.speeddiallocker.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.vitbac.speeddiallocker.R;

import java.util.ArrayList;
import java.util.List;

public class FontPickerDialogFragment extends DialogFragment
        implements AdapterView.OnItemClickListener {

    private List<FontListItem> mItems;
    private OnFontSelectedListener mListener;
    private String mFontSelected;
    private boolean mNoFontSelected;
    private int mKey;
    private View mFragView;
    private String TAG = "FontPickerDialogFragment";


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FontPickerDialogFragment() {
    }

    public static FontPickerDialogFragment newInstance(int key) {
        FontPickerDialogFragment frag = new FontPickerDialogFragment();
        Bundle bundle = new Bundle(3);
        bundle.putInt("key", key);
        bundle.putInt("fontSelected", 0);
        bundle.putBoolean("noFontSelected", true);
        frag.setArguments(bundle);
        return frag;
    }

    public static FontPickerDialogFragment newInstance(String fontSelected, int key) {
        FontPickerDialogFragment frag = new FontPickerDialogFragment();
        Bundle bundle = new Bundle(3);
        bundle.putInt("key", key);
        bundle.putString("fontSelected", fontSelected);
        bundle.putBoolean("noFontSelected", false);
        frag.setArguments(bundle);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        mKey = bundle.getInt("key");
        mFontSelected = bundle.getString("fontSelected");
        mNoFontSelected = bundle.getBoolean("noFontSelected");

        mItems = new ArrayList<FontListItem>();
        TypedArray fonts = getResources().obtainTypedArray(R.array.fonts);

        for (int i = 0; i < fonts.length(); i++) {
            mItems.add(new FontListItem(fonts.getString(i)));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFragView = inflater.inflate(R.layout.fragment_font_dialog, container, false);
        getDialog().setCanceledOnTouchOutside(true);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return mFragView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFontSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFontSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView fontList = (ListView) mFragView.findViewById(R.id.font_list);
        fontList.setOnItemClickListener(this);
        fontList.setAdapter(new FontListAdapter(getActivity(), mItems));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FontListAdapter adapter = (FontListAdapter) parent.getAdapter();
        FontListItem item = adapter.getItem(position);

        if (null != mListener) {
            mListener.onFontSelected(item.font, mKey);
            dismiss();
        }
    }


    public interface OnFontSelectedListener {
        void onFontSelected(String font, int key);
    }

    public class FontListItem {
        public String font;

        public FontListItem(String font) {
            this.font = font;
        }
    }

    public class FontListAdapter extends ArrayAdapter<FontListItem> {
        private static final int mListItemLayout = R.layout.fragment_font_list_item;

        public FontListAdapter(Context context, List<FontListItem> items) {
            super(context, mListItemLayout, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                // means a new view should be created

                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(mListItemLayout, parent, false);

                // initialize the view holder
                viewHolder = new ViewHolder();
                viewHolder.textView = (TextView) convertView.findViewById(R.id.font_list_item);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                // To prevent a reused view from holding the selected color
                viewHolder.textView.setBackgroundColor(getResources().getColor(R.color.white));
            }

            viewHolder.textView.setText(getString(R.string.font_test_text));


            FontListItem item = getItem(position);
            TypedArray fonts = getResources().obtainTypedArray(R.array.fonts);
            //String selectedFont = fonts.getString(item.font);
            viewHolder.textView
                    .setTypeface(Typeface.create(item.font, Typeface.NORMAL));

            if (!mNoFontSelected && mFontSelected.equals(item.font)) {
                viewHolder.textView.setBackgroundColor(getResources().getColor(R.color.font_selected_background));
            }

            /*if (selectedFont.equals(getString(R.string.sans_serif))) {
                viewHolder.textView.setTypeface(Typeface.SANS_SERIF);

            }
            else if (selectedFont.equals(getString(R.string.sans_serif_light))) {
                viewHolder.textView.setTypeface(Typeface.create(selectedFont), Typeface.NORMAL);

            }
            else if (selectedFont.equals(getString(R.string.sans_serif_condensed))) {

            }
            else if (selectedFont.equals(getString(R.string.monospace))) {

            }
            else if (selectedFont.equals(getString(R.string.serif))) {

            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                    && selectedFont.equals(getString(R.string.sans_serif_thin))) {

            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && selectedFont.equals(getString(R.string.sans_serif_medium))) {

            }
            else {
                Log.e(TAG, "Font returned from font picker not recognized");
            }*/





            /*if (!mNoColorSelected && item.color == mColorSelected) {
                viewHolder.colorView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD_ITALIC);
            } else {
                // To prevent recycling of views with remnants of bold italic
                viewHolder.colorView.setTypeface(null, Typeface.NORMAL);
            }*/

            return convertView;
        }

        private class ViewHolder {
            TextView textView;
        }
    }

}
