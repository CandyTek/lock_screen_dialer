package com.vitaminbacon.lockscreendialer.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.vitaminbacon.lockscreendialer.R;

import java.util.ArrayList;
import java.util.List;

public class ColorPickerDialogFragment extends DialogFragment
        implements AdapterView.OnItemClickListener {

    private List<ColorListItem> mItems;
    private OnColorSelectedListener mListener;
    private int mColorSelected;
    private boolean mNoColorSelected;
    private int mKey;
    private View mFragView;
    private String TAG = "ColorPickerDialogFragment";


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ColorPickerDialogFragment() {
    }

    public static ColorPickerDialogFragment newInstance(int key) {
        ColorPickerDialogFragment frag = new ColorPickerDialogFragment();
        Bundle bundle = new Bundle(3);
        bundle.putInt("key", key);
        bundle.putInt("colorSelected", 0);
        bundle.putBoolean("noColorSelected", true);
        frag.setArguments(bundle);
        return frag;
    }

    public static ColorPickerDialogFragment newInstance(int colorSelected, int key) {
        ColorPickerDialogFragment frag = new ColorPickerDialogFragment();
        Bundle bundle = new Bundle(3);
        bundle.putInt("key", key);
        bundle.putInt("colorSelected", colorSelected);
        bundle.putBoolean("noColorSelected", false);
        frag.setArguments(bundle);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();

        mKey = bundle.getInt("key");
        mColorSelected = bundle.getInt("colorSelected");
        mNoColorSelected = bundle.getBoolean("noColorSelected");

        mItems = new ArrayList<ColorListItem>();
        TypedArray colors = getResources().obtainTypedArray(R.array.color_value_list);
        TypedArray names = getResources().obtainTypedArray(R.array.color_name_list);
        int length;

        if (colors.length() != names.length()) {
            Log.e(TAG, "Resource array of names and colors do not match");
            // In this weird case, just constrain list to the smaller of the two
            length = colors.length() < names.length() ? colors.length() : names.length();
        } else {
            length = colors.length();
        }

        for (int i = 0; i < length; i++) {
            mItems.add(new ColorListItem(colors.getColor(i, 0), names.getString(i)));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFragView = inflater.inflate(R.layout.fragment_color, container, false); // How can this be right?
        getDialog().setCanceledOnTouchOutside(true);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        /*// Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);*/

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
            mListener = (OnColorSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnColorSelectedListener");
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

        ListView colorList = (ListView) mFragView.findViewById(R.id.color_list);
        colorList.setOnItemClickListener(this);
        colorList.setAdapter(new ColorListAdapter(getActivity(), mItems));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ColorListAdapter adapter = (ColorListAdapter) parent.getAdapter();
        ColorListItem item = adapter.getItem(position);

        if (null != mListener) {
            mListener.onColorSelected(item.color, mKey);
            dismiss();
        }
    }


    public interface OnColorSelectedListener {
        public void onColorSelected(int color, int key);
    }

    public class ColorListItem {
        public int color;
        public String name;

        public ColorListItem(int color, String name) {
            this.color = color;
            this.name = name;
        }
    }

    public class ColorListAdapter extends ArrayAdapter<ColorListItem> {
        private static final int mListItemLayout = R.layout.fragment_color_list_item;

        public ColorListAdapter(Context context, List<ColorListItem> items) {
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
                viewHolder.colorView = (TextView) convertView.findViewById(R.id.color_list_item);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            ColorListItem item = getItem(position);
            viewHolder.colorView.setText(item.name);
            viewHolder.colorView.setBackgroundColor(item.color);
            viewHolder.colorView.setTextColor(getResources().getColor(R.color.white));
            viewHolder.colorView.setShadowLayer(3, 1, 1, getResources().getColor(R.color.black_cow));

            /*TypedArray darkTypeList =
                    getResources().obtainTypedArray(R.array.color_req_dark_type_list);
            boolean useDarkTextColor = false;
            for (int i=0; i < darkTypeList.length(); i++) {
                if (item.color == darkTypeList.getColor(i, getResources().getColor(R.color.white))) {
                    useDarkTextColor = true;
                    break;
                }
            }
            if (!useDarkTextColor) {
                viewHolder.colorView.setTextColor(getResources().getColor(R.color.white));
            } else {
                viewHolder.colorView.setTextColor(getResources().getColor(R.color.white));
                viewHolder.colorView.setShadowLayer(4, 1, 1, Color.BLACK);
            }*/
            if (!mNoColorSelected && item.color == mColorSelected) {
                viewHolder.colorView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD_ITALIC);
            } else {
                // To prevent recycling of views with remnants of bold italic
                viewHolder.colorView.setTypeface(null, Typeface.NORMAL);
            }

            return convertView;
        }

        private class ViewHolder {
            TextView colorView;
        }
    }

}
