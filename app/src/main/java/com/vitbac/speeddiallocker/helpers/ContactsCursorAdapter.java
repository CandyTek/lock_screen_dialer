package com.vitbac.speeddiallocker.helpers;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.vitbac.speeddiallocker.R;

import static android.provider.ContactsContract.CommonDataKinds;

/**
 * Created by nick on 5/14/15.
 * Modification of SimpleCursorAdapter to:
 * (1) Replace TextViews displaying PhoneType codes with their English equivalents;
 * (2) Replace non-existing thumbnails with the default thumbnail pic
 */

public class ContactsCursorAdapter extends SimpleCursorAdapter {

    public ContactsCursorAdapter(Context context, int layout, Cursor c,
                                 String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewBinder binder = getViewBinder();
        final int count = mTo.length;
        final int[] from = mFrom;
        final int[] to = mTo;

        for (int i = 0; i < count; i++) {
            final View v = view.findViewById(to[i]);
            if (v != null) {
                boolean bound = false;
                if (binder != null) {
                    bound = binder.setViewValue(v, cursor, from[i]);

                }
                if (!bound) {
                    String text = cursor.getString(from[i]);
                    if (text == null) {
                        text = "";
                    }
                    if (v instanceof TextView) {
                        // Modify to convert the phone number type to readable
                        if (((TextView) v).getId() == R.id.phone_selector_list_item_phone_type)
                            setViewText((TextView) v, convertPhoneTypeToReadableText(text, context));
                        else {
                            setViewText((TextView) v, text);
                        }
                    } else if (v instanceof ImageView) {
                        // Modify to set the thumbnail to the default thumbnail where appropriate
                        Log.d("ContactsCursorAdapter", text);
                        if (((ImageView) v).getId() == R.id.contact_selector_list_item_pic &&
                                text == ""){
                            setViewImage((ImageView) v,
                                    String.valueOf(R.drawable.default_contact_image));
                        }
                        else {
                            setViewImage((ImageView) v, text);
                        }
                    } else {
                        throw new IllegalStateException(v.getClass().getName() +
                                "is not a view that can be bound by this ContactsAdapter.");
                    }

                }
            }
        }


    }

    private String convertPhoneTypeToReadableText(String text, Context context) {

        int phoneTypeCode;
        String returnString;

        // Convert the text to an int so it can go through the switch statement
        try {
            phoneTypeCode = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            Log.d("Phone Type Error", "Improper contents of " + text);
            phoneTypeCode = CommonDataKinds.Phone.TYPE_OTHER;
        }

        switch (phoneTypeCode) {
            case CommonDataKinds.Phone.TYPE_CUSTOM:
                returnString = context.getString(R.string.phone_type_custom);
                break;
            case CommonDataKinds.Phone.TYPE_HOME:
                returnString = context.getString(R.string.phone_type_home);
                break;
            case CommonDataKinds.Phone.TYPE_MOBILE:
                returnString = context.getString(R.string.phone_type_mobile);
                break;
            case CommonDataKinds.Phone.TYPE_WORK:
                returnString = context.getString(R.string.phone_type_work);
                break;
            case CommonDataKinds.Phone.TYPE_FAX_WORK:
                returnString = context.getString(R.string.phone_type_fax_work);
                break;
            case CommonDataKinds.Phone.TYPE_FAX_HOME:
                returnString = context.getString(R.string.phone_type_fax_home);
                break;
            case CommonDataKinds.Phone.TYPE_PAGER:
                returnString = context.getString(R.string.phone_type_pager);
                break;
            case CommonDataKinds.Phone.TYPE_CALLBACK:
                returnString = context.getString(R.string.phone_type_callback);
                break;
            case CommonDataKinds.Phone.TYPE_CAR:
                returnString = context.getString(R.string.phone_type_car);
                break;
            case CommonDataKinds.Phone.TYPE_COMPANY_MAIN:
                returnString = context.getString(R.string.phone_type_company_main);
                break;
            case CommonDataKinds.Phone.TYPE_ISDN:
                returnString = context.getString(R.string.phone_type_isdn);
                break;
            case CommonDataKinds.Phone.TYPE_MAIN:
                returnString = context.getString(R.string.phone_type_main);
                break;
            case CommonDataKinds.Phone.TYPE_OTHER_FAX:
                returnString = context.getString(R.string.phone_type_other_fax);
                break;
            case CommonDataKinds.Phone.TYPE_RADIO:
                returnString = context.getString(R.string.phone_type_radio);
                break;
            case CommonDataKinds.Phone.TYPE_TELEX:
                returnString = context.getString(R.string.phone_type_telex);
                break;
            case CommonDataKinds.Phone.TYPE_TTY_TDD:
                returnString = context.getString(R.string.phone_type_tty_tdd);
                break;
            case CommonDataKinds.Phone.TYPE_WORK_MOBILE:
                returnString = context.getString(R.string.phone_type_work_mobile);
                break;
            case CommonDataKinds.Phone.TYPE_WORK_PAGER:
                returnString = context.getString(R.string.phone_type_work_pager);
                break;
            case CommonDataKinds.Phone.TYPE_ASSISTANT:
                returnString = context.getString(R.string.phone_type_assistant);
                break;
            case CommonDataKinds.Phone.TYPE_MMS:
                returnString = context.getString(R.string.phone_type_mms);
                break;
            default:
                returnString = context.getString(R.string.phone_type_other);
                break;
        }

        return returnString;
    }
}
