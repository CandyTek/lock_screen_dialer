package com.vitbac.speeddiallocker.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public static void resizeBitmapToNewFile (Context context, String fromFilePath,
                                              String toFileName, int orientation, int width,
                                              int height, RectF crop, int scaleW, int scaleH)
            throws FileNotFoundException, IOException {
        if (fromFilePath == null) {
            throw new FileNotFoundException("File path is null");
        }
        File file = new File(fromFilePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File " + fromFilePath + " does not exist");
        }

        Bitmap bmp = decodeSampledBitmapFromFile(fromFilePath, orientation, width, height,
                crop, scaleW, scaleH);
        Log.d(TAG, "Bitmap size=(" + bmp.getWidth() + ", " + bmp.getHeight() + ")");

        float scalerW = ((float) bmp.getWidth())/scaleW;
        float scalerH = ((float) bmp.getHeight())/scaleH;
        int startX = (int) (crop.left * scalerW);
        int startY = (int) (crop.top * scalerH);
        int cropWidth = (int) ((crop.right - crop.left) * scalerW);
        int cropHeight = (int) ((crop.bottom - crop.top) * scalerH);
        Log.d(TAG, "Crop coords: (" + startX + ", " + startY + ") w=" + cropWidth + " h=" + cropHeight);

        // TODO: make this robust -- if x+w > bitmap size, just max it at bitmap size, etc.
        bmp= Bitmap.createBitmap(
                bmp,
                startX, // start x coordinate
                startY, // start y coordinate
                cropWidth, // width
                cropHeight // height
        );
        if (bmp == null) {
            throw new FileNotFoundException("Bitmap could not be obtained from file " + fromFilePath);
        }

        FileOutputStream out = null;
        try {
            out = context.openFileOutput(toFileName, Context.MODE_PRIVATE);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }

    }

    private static Bitmap decodeSampledBitmapFromFile(String filePath, int orientation,
                                                     int maxWidth, int maxHeight) {
        return decodeSampledBitmapFromFile(filePath, orientation, maxWidth, maxHeight, null, 0, 0);
    }

    /**
     * Decodes a bitmap from a file.  If crop is not null, crop will contain the coordinates of the
     * desired cropped bitmap, scaled to screenWidth and screenHeight.  Crop needs to be rescaled
     * to fit the bitmap's actual size
     */
    private static Bitmap decodeSampledBitmapFromFile(String filePath, int orientation,
                                                      int screenWidth, int screenHeight, RectF crop,
                                                      int scaleW, int scaleH) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, screenWidth, screenHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        if (bitmap != null) {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true);
            /*if (crop == null) {
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                        matrix, true);
            } else {
                Log.d(TAG, "Bitmap size=(" + bitmap.getWidth() + ", " + bitmap.getHeight() + ")");
                Log.d(TAG, "Cropping bitmap: ("
                        + (int) crop.left + ", "
                        + (int) crop.top + ", "
                        + (int) crop.right + ", "
                        + (int) crop.bottom + ")");

                float scalerW = ((float) bitmap.getWidth())/scaleW;
                float scalerH = ((float) bitmap.getHeight())/scaleH;
                return Bitmap.createBitmap(
                        bitmap,
                        (int) (crop.left * scalerW), // start x coordinate
                        (int) (crop.top * scalerH), // start y coordinate
                        (int) ((crop.right - (int) crop.left) * scalerW), // width
                        (int) ((crop.bottom - (int) crop.top) * scalerH), // height
                        matrix, true);
            }*/
        } else {
            Log.e(TAG, "Unable to obtain bitmap from file path");
            return null;
        }

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
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    /**
     * Interface assists an activity in obtaining a bitmap from the async task
     */
    public interface GetBitmapFromTaskInterface {
        void getBitmapFromTask(Bitmap bitmap);
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
                // TODO: create a default error background
            }

        }
    }

    private static class DataToBitmapTask extends AsyncTask<Integer, Void, Bitmap> {
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
