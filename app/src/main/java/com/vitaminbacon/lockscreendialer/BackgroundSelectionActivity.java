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

import yuku.ambilwarna.AmbilWarnaDialog;


public class BackgroundSelectionActivity extends ActionBarActivity implements View.OnClickListener {

    private final static String TAG = "BackgroundSelActivity";
    private final static int PICK_IMAGE = 1; // Request code to be handled in onActivityResult()
    //private final static int PICK_COLOR = 2;
    private final static int NO_COLOR = -1;
    private final static int ORIENTATION_UNKNOWN = -1;

    //private ViewGroup mRootViewGroup;
    private ImageView mCurrentBackgroundView;
    private Button mPicsSelectButton;
    private Button mColorsSelectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_selection);
        //mRootViewGroup = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        mCurrentBackgroundView =
                (ImageView) findViewById(R.id.background_selection_current_background);
        mPicsSelectButton = (Button) findViewById(R.id.background_selection_pics_button);
        mColorsSelectButton = (Button) findViewById(R.id.background_selection_colors_button);
        mPicsSelectButton.setOnClickListener(this);
        mColorsSelectButton.setOnClickListener(this);
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
                Uri selectedImage = data.getData();
                String filePath = getBitmapFilePath(selectedImage);

                if (filePath == null) { // unable to access filePath
                    makeToast(getString(R.string.toast_background_selection_file_access_error));
                    Log.d(TAG, "Unable to obtain file path from uri: " + selectedImage.toString());
                    return;
                }

                SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(getString(R.string.key_background_pic), filePath);
                int orientation = getBitmapOrientation(selectedImage, ORIENTATION_UNKNOWN);
                editor.putInt(getString(R.string.key_background_orientation), orientation);
                editor.remove(getString(R.string.key_background_color)); // used to flag whether to display pic or color
                editor.commit();
                Log.d(TAG, "Stored file path " + filePath + " and orientation " + orientation);
            } else {
                Log.d(TAG, "onActivityResult pick image received result code " + resultCode);
            }
        } /*else if (requestCode == PICK_COLOR) {
            if (resultCode == RESULT_OK) {

                int colorResId = data.getIntExtra(ColorSelectionActivity.RESULT, NO_COLOR);
                SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(getString(R.string.key_background_color), colorResId);
                editor.commit();

            } else {
                Log.d(TAG, "onActivityResult pick color received result code " + resultCode);
            }
        }*/
        else {
            Log.e(TAG, "onActivityResult received unknown requestCode");
        }
    }

    // TODO: background remove button, reverting to default, and select simple color
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.background_selection_pics_button:
                pickBackgroundFromGallery();
                break;
            case R.id.background_selection_colors_button:
                //pickColorsFromActivity();
                SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                int color = prefs.getInt(getString(R.string.key_background_color),
                        getResources().getColor(R.color.ripple_material_light));

                AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, color,
                        new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {
                        // Do nothing
                    }

                    @Override
                    public void onOk(AmbilWarnaDialog ambilWarnaDialog, int i) {
                        mCurrentBackgroundView.setImageBitmap(null);
                        mCurrentBackgroundView.setBackgroundColor(i);
                        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(getString(R.string.key_background_color), i);
                        editor.commit();
                    }
                });
                dialog.show();
                break;
        }
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

    /*private void pickColorsFromActivity() {
        Intent pickColorsIntent = new Intent(this, ColorSelectionActivity.class);
        startActivityForResult(pickColorsIntent, PICK_COLOR);
    }*/

    private void setBackgroundBitmap() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String path = prefs.getString(getString(R.string.key_background_pic), null);
        int color = prefs.getInt(getString(R.string.key_background_color), NO_COLOR);
        int orientation = prefs.getInt(
                getString(R.string.key_background_orientation), ORIENTATION_UNKNOWN);

        if (path == null && color == NO_COLOR) { // meaning no background has been selected

            mCurrentBackgroundView.setImageBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.background_default));
            //return BitmapFactory.decodeResource(getResources(), R.drawable.background_default);
        } else if (color != NO_COLOR) {
            mCurrentBackgroundView.setImageBitmap(null);
            mCurrentBackgroundView.setBackgroundColor(color);
        } else {
            Log.d(TAG, "assessing image Uri from stored data");

            Display display = getWindowManager().getDefaultDisplay();
            Bitmap bitmap;
            int w, h;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                Point size = new Point();
                display.getSize(size);
                w = size.x;
                h = size.y;
            } else {
                w = display.getWidth();
                h = display.getHeight();
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
