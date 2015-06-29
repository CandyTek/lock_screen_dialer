package com.vitaminbacon.lockscreendialer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.vitaminbacon.lockscreendialer.fragments.ColorPickerDialogFragment;
import com.vitaminbacon.lockscreendialer.helpers.BitmapToViewHelper;


public class BackgroundSelectionActivity extends ActionBarActivity implements View.OnClickListener,
        ColorPickerDialogFragment.OnColorSelectedListener {


    private final static String TAG = "BackgroundSelActivity";
    private final static int PICK_IMAGE = 1; // Request code to be handled in onActivityResult()
    //private final static int PICK_COLOR = 2;
    private final static int NO_COLOR = -1;
    private final static int ORIENTATION_UNKNOWN = -1;

    //private ViewGroup mRootViewGroup;
    /*private ImageView mCurrentBackgroundView;
    private Button mPicsSelectButton;
    private Button mColorsSelectButton;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_selection);
        //mRootViewGroup = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        /*mCurrentBackgroundView =
                (ImageView) findViewById(R.id.background_selection_current_background);*/
        /*mPicsSelectButton = (Button) findViewById(R.id.background_selection_pics_button);
        mColorsSelectButton = (Button) findViewById(R.id.background_selection_colors_button);
        mPicsSelectButton.setOnClickListener(this);
        mColorsSelectButton.setOnClickListener(this);
        mCurrentBackgroundView.setImageBitmap(null);*/

        Button picsSelectButton = (Button) findViewById(R.id.background_selection_pics_button);
        Button colorsSelectButton = (Button) findViewById(R.id.background_selection_colors_button);
        picsSelectButton.setOnClickListener(this);
        colorsSelectButton.setOnClickListener(this);

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
                String filePath = BitmapToViewHelper.getBitmapFilePath(this, selectedImage);

                if (filePath == null) { // unable to access filePath
                    makeToast(getString(R.string.toast_background_selection_file_access_error));
                    Log.d(TAG, "Unable to obtain file path from uri: " + selectedImage.toString());
                    return;
                }

                SharedPreferences prefs = getSharedPreferences(
                        getString(R.string.background_file_key),
                        MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(getString(R.string.key_background_pic), filePath);
                int orientation = BitmapToViewHelper.getBitmapOrientation(
                        this,
                        selectedImage,
                        ORIENTATION_UNKNOWN);
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

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.background_selection_pics_button:
                pickBackgroundFromGallery();
                break;
            case R.id.background_selection_colors_button:
                //pickColorsFromActivity();
                SharedPreferences prefs = getSharedPreferences(
                        getString(R.string.background_file_key),
                        MODE_PRIVATE);
                int color = prefs.getInt(getString(R.string.key_background_color),
                        getResources().getColor(R.color.ripple_material_light));

                ColorPickerDialogFragment dialog = ColorPickerDialogFragment.newInstance(color);
                dialog.show(getFragmentManager(), "fragment_color_list_dialog");
                /*AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, color,
                        new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {
                        // Do nothing
                    }

                    @Override
                    public void onOk(AmbilWarnaDialog ambilWarnaDialog, int i) {
                        ImageView bg = (ImageView) findViewById(
                                R.id.background_selection_current_background);
                        bg.setImageBitmap(null);  //perhaps consider .setImageDrawable(null)?
                        bg.setBackgroundColor(i);
                        SharedPreferences sharedPreferences = getSharedPreferences(
                                getString(R.string.background_file_key),
                                MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(getString(R.string.key_background_color), i);
                        editor.commit();
                    }
                });
                dialog.show(); */
                break;


        }
    }

    public void onColorSelected(int color) {
        Log.d(TAG, "color selected = " + color);
        ImageView bg = (ImageView) findViewById(
                R.id.background_selection_current_background);
        bg.setImageBitmap(null);  //perhaps consider .setImageDrawable(null)?
        bg.setBackgroundColor(color);
        SharedPreferences sharedPreferences = getSharedPreferences(
                getString(R.string.background_file_key),
                MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.key_background_color), color);
        editor.commit();
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
        SharedPreferences prefs = getSharedPreferences(
                getString(R.string.background_file_key),
                MODE_PRIVATE);
        String path = prefs.getString(getString(R.string.key_background_pic), null);
        int color = prefs.getInt(getString(R.string.key_background_color), NO_COLOR);
        int orientation = prefs.getInt(
                getString(R.string.key_background_orientation), ORIENTATION_UNKNOWN);
        ImageView bg = (ImageView) findViewById(
                R.id.background_selection_current_background);

        Log.d(TAG, "path = " + path);
        if (path == null && color == NO_COLOR) { // meaning no background has been selected

            bg.setImageBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.background_default));
            //return BitmapFactory.decodeResource(getResources(), R.drawable.background_default);
        } else if (color != NO_COLOR) {
            bg.setImageBitmap(null);  // TODO: consider setImageDrawable(null)
            bg.setBackgroundColor(color);
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
            BitmapToViewHelper.assignViewWithBitmap(bg, path, orientation, w, h);
            /*BitmapWorkerTask task = new BitmapWorkerTask(mCurrentBackgroundView, path);
            task.execute(orientation, w, h);*/
            //return bitmap;
        }
    }
}
