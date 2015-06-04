package com.vitaminbacon.lockscreendialer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.lang.ref.WeakReference;


public class BackgroundSelectionActivity extends ActionBarActivity implements View.OnClickListener {

    private final static String TAG = "BackgroundSelActivity";
    private final static int PICK_IMAGE = 1; // Request code to be handled in onActivityResult()
    private final static int ORIENTATION_UNKNOWN = -1;

    //private ViewGroup mRootViewGroup;
    private ImageView mCurrentBackgroundView;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_selection);
        //mRootViewGroup = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        mCurrentBackgroundView =
                (ImageView) findViewById(R.id.backgournd_selection_current_background);
        mButton = (Button) findViewById(R.id.background_selection_button);
        mButton.setOnClickListener(this);
        mCurrentBackgroundView.setImageBitmap(null);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mRootViewGroup.setBackground(getCurrentBackground());
        } else {
            mRootViewGroup.setBackgroundDrawable(getCurrentBackground());
        }*/

    }

    @Override
    protected void onResume() {
        super.onResume();
        setBackgroundBitmap();
        //mCurrentBackgroundView.setImageBitmap(setBackgroundBitmap());
        //mCurrentBackgroundView.setImageURI(setBackgroundBitmap());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_background_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                /*Uri uri = data.getData();
                *//*try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "URI did not identify valid file.");
                    return;
                } catch (IOException f) {
                    Log.e(TAG, "IOException - " + f.toString());
                }*//*
                if (uri == null) {
                    Log.e(TAG, "onActivityResult received null data from intent");
                    return;
                }
                File f = new File(uri.getPath());
                if (!f.isFile()) {
                    Log.d(TAG, "intent returned file that does not exist, toString " + uri.toString() + ", getPath " + uri.getPath());
                }
                if (f.exists()) {
                    Log.d(TAG, "intent returned file that does exist");
                }*/

                Uri selectedImage = data.getData();
                String filePath = getBitmapFilePath(selectedImage);

                if (filePath == null) { // unable to access filePath
                    makeToast(getString(R.string.toast_background_selection_file_access_error));
                    Log.d(TAG, "Unable to obtain file path from uri: " + selectedImage.toString());
                    return;
                }

                SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(getString(R.string.key_background), filePath);

                int orientation = getBitmapOrientation(selectedImage, ORIENTATION_UNKNOWN);
                editor.putInt(getString(R.string.key_background_orientation), orientation);
                editor.commit();
                Log.d(TAG, "Stored file path " + filePath + " and orientation " + orientation);
            } else {
                Log.d(TAG, "onActivityResult pick image received result code " + resultCode);
            }
        } else {
            Log.e(TAG, "onActivityResult received unknown requestCode");
        }
    }

    // TODO: background remove button, reverting to default, and select simple color
    public void onClick(View view) {
        pickBackgroundFromGallery();
    }

    /***********************************************************************************************
     * Private methods
     * *********************************************************************************************
     */

    private void makeToast (String string) {
        Toast toast = Toast.makeText(this, string, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void pickBackgroundFromGallery() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(
                Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    private void setBackgroundBitmap() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String path = prefs.getString(getString(R.string.key_background), null);
        int orientation = prefs.getInt(
                getString(R.string.key_background_orientation), ORIENTATION_UNKNOWN);

        if (path == null) { // meaning no background has been selected
            /*return Uri.parse( "android.resource://"
                    + getString(R.string.package_name)
                    + "/"
                    + R.drawable.background_default);*/
            mCurrentBackgroundView.setImageBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.background_default));
            //return BitmapFactory.decodeResource(getResources(), R.drawable.background_default);
        } else {
            Log.d(TAG, "assessing image Uri from stored data");
            //BitmapFactory.Options options = new BitmapFactory.Options();
            //int imageHeight, imageWidth;

            // get the dimensions
            /*options.inJustDecodeBounds = true; // indicates we just want to find the dimensions of the beast
            BitmapFactory.decodeFile( (new File(path)).getAbsolutePath(), options);
            imageHeight = options.outHeight;
            imageWidth = options.outWidth;*/

            //return Uri.parse(path);
            Display display = getWindowManager().getDefaultDisplay();
            Bitmap bitmap;
            int w, h;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                Point size = new Point();
                display.getSize(size);
                w = size.x;
                h = size.y;
                /*bitmap = decodeSampledBitmapFromFile(path, orientation, size.x, size.y);*/
            } else {
                w = display.getWidth();
                h = display.getHeight();
                /*bitmap = decodeSampledBitmapFromFile(path, orientation,
                        display.getWidth(), display.getHeight());*/
            }
            BitmapWorkerTask task = new BitmapWorkerTask(mCurrentBackgroundView, path);
            task.execute(orientation, w, h);
            //return bitmap;
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

    public static Bitmap decodeSampledBitmapFromFile(String filePath, int orientation,
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

    /**
     * Returns the orientation of a photo, or the defaultReturn if unknown
     * @param photoUri
     * @param defaultReturn
     * @return
     */
    private int getBitmapOrientation (Uri photoUri, int defaultReturn) {
        Cursor cursor = this.getContentResolver().query(photoUri,
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
     * Takes a URI and does magic to return the actual absolute file path of the image
     * @param photoUri
     * @return
     */
    private String getBitmapFilePath (Uri photoUri) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        // Get the cursor
        Cursor cursor = getContentResolver().query(photoUri,
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

    private class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private final WeakReference<String> filePathReference;
        private int orientation = 0;
        private int width = 0;
        private int height = 0;

        public BitmapWorkerTask(ImageView imageView, String filePath) {
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
                final ImageView imageView = imageViewReference.get();
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
    /*private Drawable getCurrentBackground() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String filePath = prefs.getString(getString(R.string.key_background), null);
        if (filePath == null) { // meaning no background has been selected
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return getDrawable(R.drawable.background_default);
            }
            else {
                return getResources().getDrawable(R.drawable.background_default);
            }
        } else {
            File f = new File(filePath);
            return Drawable.createFromPath(f.getAbsolutePath());
        }
    }*/


}
