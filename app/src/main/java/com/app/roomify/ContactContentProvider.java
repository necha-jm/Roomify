package com.app.roomify;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class ContactContentProvider extends ContentProvider {

    public static final String AUTHORITY = "com.app.roomify.provider";
    public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/contacts");

    private DBHelper dbHelper;

    @Override
    public boolean onCreate() {
        Context context = getContext();

        if (context == null) {
            return false;
        }

        dbHelper = new DBHelper(context);
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long id = db.insert(DBHelper.TABLE_NAME, null, values);

        Uri newUri = Uri.parse(CONTENT_URI + "/" + id);

        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(newUri, null);
        }

        return newUri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        return db.query(DBHelper.TABLE_NAME, projection,
                selection, selectionArgs,
                null, null, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values,
                      String selection, String[] selectionArgs) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int count = db.update(DBHelper.TABLE_NAME, values,
                selection, selectionArgs);

        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @Override
    public int delete(Uri uri, String selection,
                      String[] selectionArgs) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int count = db.delete(DBHelper.TABLE_NAME,
                selection, selectionArgs);

        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
}