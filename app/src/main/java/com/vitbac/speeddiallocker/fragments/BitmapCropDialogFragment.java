package com.vitbac.speeddiallocker.fragments;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.vitbac.speeddiallocker.R;
import com.vitbac.speeddiallocker.helpers.BitmapToViewHelper;
import com.vitbac.speeddiallocker.views.CropImageView;

import java.io.File;

/**
 * Created by nick on 7/24/15.
 */
public class BitmapCropDialogFragment extends DialogFragment
        implements BitmapToViewHelper.GetBitmapFromTaskInterface, View.OnClickListener{

    private static final String TAG = "BitmapCropDialogFrag";
    Bitmap mBitmap;
    CropImageView mCropImageView;
    String mFilePath;
    int mOrientation;
    Paint mPaint;
    GetBitmapCrop mListener;

    public interface GetBitmapCrop {
        void onBitmapCropSelect(RectF rectF, int scaleW, int scaleH, String fileName, int orientation);
        void onBitmapCropCancel();
    }
    public BitmapCropDialogFragment() {
    }

    public static BitmapCropDialogFragment newInstance(String filePath, int orientation) {
        BitmapCropDialogFragment frag = new BitmapCropDialogFragment();
        Bundle bundle = new Bundle(2);
        bundle.putString("filePath", filePath);
        bundle.putInt("orientation", orientation);
        frag.setArguments(bundle);
        return frag;
    }

    public static BitmapCropDialogFragment newInstance(String filePath, int orientation,
                                                       int color, float stroke) {
        BitmapCropDialogFragment frag = new BitmapCropDialogFragment();
        Bundle bundle = new Bundle(4);
        bundle.putString("filePath", filePath);
        bundle.putInt("orientation", orientation);
        bundle.putInt("color", color);
        bundle.putFloat("stroke", stroke);
        frag.setArguments(bundle);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        mFilePath = bundle.getString("filePath");
        mOrientation = bundle.getInt("orientation");
        int color = bundle.getInt("color", -1);
        float stroke = bundle.getFloat("stroke", -1);
        if (color != -1 && stroke > 0) {
            mPaint = new Paint();
            mPaint.setColor(color);
            mPaint.setStrokeWidth(stroke);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bitmap_crop_dialog, container, false);
        mCropImageView = (CropImageView) view.findViewById(R.id.crop_image_view);
        view.findViewById(R.id.OK_crop).setOnClickListener(this);
        view.findViewById(R.id.cancel_crop).setOnClickListener(this);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        File file = null;
        if (mFilePath != null) {
            file = new File(mFilePath);
        }
        Log.d(TAG, "filepath=" + mFilePath);
        if (file != null && file.exists()) {
            int w = getDisplayWidth();
            int h = getDisplayHeight();
            BitmapToViewHelper.getBitmapFromFile(this, mFilePath, mOrientation, w, h);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        /*if (mBitmap != null) {
            mBitmap.recycle();
        }*/

        super.onDestroy();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.OK_crop:
                if(mListener != null) {
                    mListener.onBitmapCropSelect(
                            mCropImageView.getCrop(),
                            mCropImageView.getWidth(),
                            mCropImageView.getHeight(),
                            mFilePath,
                            mOrientation
                    );
                }
                break;
            case R.id.cancel_crop:
                if (mListener != null) {
                    mListener.onBitmapCropCancel();
                }
                break;
            default:
                return;
        }
        CropImageView cropImageView = (CropImageView) getDialog().findViewById(R.id.crop_image_view);
        if (cropImageView != null) {
            Drawable drawable = cropImageView.getDrawable();
            if(drawable instanceof BitmapDrawable) {
                BitmapDrawable bmd = (BitmapDrawable) drawable;
                bmd.getBitmap().recycle();
                cropImageView.setImageBitmap(null);
            }
        }
        dismiss();
    }



    public void getBitmapFromTask(Bitmap bitmap) {
        Log.d(TAG, "Retrieved bitmap");
        //mBitmap = bitmap;
        CropImageView cropImageView = (CropImageView) getDialog().findViewById(R.id.crop_image_view);
        if (cropImageView != null) {
            cropImageView.setBitmapWithCrop(bitmap, getDisplayWidth(), getDisplayHeight(), mPaint);
        }
    }

    public void setBitmapCropListener(GetBitmapCrop listener) {
        mListener = listener;
    }

    private int getDisplayWidth() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();

        // Get the right screen size in manner depending on version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            return size.x;

        } else {
            return display.getWidth();
        }
    }

    private int getDisplayHeight() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();

        // Get the right screen size in manner depending on version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            return size.y;

        } else {
            return display.getHeight();
        }
    }
}
