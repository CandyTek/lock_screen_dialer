package com.vitaminbacon.lockscreendialer;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Created by nick on 6/4/15.
 */
public final class BitmapToViewHelper {

    private static final String TAG = "BitmapToViewHelper";
    /**
     * Provides insurance in case the constructor is accidentally called from within the class
     */
    private BitmapToViewHelper() {
        throw new AssertionError();
    }
    /**
     * Constructor function
     * @param view - the view that should be set with a bitmap
     * @param filePath - the absolute file path to the bitmap
     * @param orientation - the orientation of the bitmap (0 for none)
     * @param width - the width of the view that it is to be set to
     * @param height - the height of the view that it is to be set to
     */

    public static void assignViewWithBitmap (ImageView view, String filePath, int orientation,
                                          int width, int height) {
        BitmapToViewTask task = new BitmapToViewTask(view, filePath);
        task.execute(orientation, width, height);
    }

    public static void assignBitmapWithData (GetBitmapFromTaskInterface activityInterface,
                                             String filePath, int orientation, int width, int height) {
        DataToBitmapTask task = new DataToBitmapTask(activityInterface, filePath);
        task.execute(orientation, width, height);
    }

    private static Bitmap decodeSampledBitmapFromFile(String filePath, int orientation,
                                                     int maxWidth, int maxHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Takes a URI and does magic to return the actual absolute file path of the image
     * @param context - the context, usually just the activity itself
     * @param photoUri - the uri of the photo
     * @return
     */
    public static String getBitmapFilePath (Context context, Uri photoUri) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        // Get the cursor
        Cursor cursor = context.getContentResolver().query(photoUri,
                filePathColumn, null, null, null);
        // Move to first row
        if (cursor == null || cursor.getCount() < 1) {
            return null;
        }

        cursor.moveToFirst();


        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String imgDecodableString = cursor.getString(columnIndex);
        cursor.close();
        return imgDecodableString;
    }

    /**
     * Returns the orientation of a photo, or the defaultReturn if unknown
     * @param photoUri
     * @param defaultReturn
     * @return
     */
    public static int getBitmapOrientation (Context context, Uri photoUri, int defaultReturn) {
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION },
                null, null, null);

        if ( cursor == null || cursor.getCount() != 1) {
            return defaultReturn;
        }

        cursor.moveToFirst();
        int orientation = cursor.getInt(0);
        cursor.close();
        return orientation;
    }

    /**
     * Interface assists an activity in obtaining a bitmap from the async task
     */
    public interface GetBitmapFromTaskInterface {
        public void getBitmapFromTask(Bitmap bitmap);
    }

    private static class BitmapToViewTask extends AsyncTask<Integer, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private final WeakReference<String> filePathReference;
        private int orientation = 0;
        private int width = 0;
        private int height = 0;

        public BitmapToViewTask(ImageView imageView, String filePath) {
            // Use a WeakReference to ensure the ImageView can't be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
            filePathReference = new WeakReference<String>(filePath);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {

            orientation = params[0];
            width = params[1];
            height = params[2];
            if (filePathReference.get() == null) {
                return null;
            }
            return decodeSampledBitmapFromFile(filePathReference.get(), orientation, width, height);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
                else {
                    Log.e(TAG, "Async received null imageView onPostExecute");
                }
            }
            else {
                Log.e(TAG, "Async received null bitmap onPostExecute");
            }

        }
    }

    private static class DataToBitmapTask extends AsyncTask<Integer, Void, Bitmap> {  // TODO: implement onProgressUpdate setting second generic type to integer?
        private final WeakReference<GetBitmapFromTaskInterface> activityInterfaceReference;
        private final WeakReference<String> filePathReference;
        private int orientation = 0;
        private int width = 0;
        private int height = 0;

        public DataToBitmapTask(GetBitmapFromTaskInterface activityInterface, String filePath) {
            activityInterfaceReference = new WeakReference<GetBitmapFromTaskInterface>(activityInterface);
            filePathReference = new WeakReference<String>(filePath);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {

            orientation = params[0];
            width = params[1];
            height = params[2];
            if (filePathReference.get() == null) {
                return null;
            }
            return decodeSampledBitmapFromFile(filePathReference.get(), orientation, width, height);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (bitmap != null) {
                activityInterfaceReference.get().getBitmapFromTask(bitmap);
            }
            else {
                Log.d(TAG, "Bitmap calculations undertaken to produce null value");
            }
            //activityInterface = null; // necessary?
        }
    }
}
