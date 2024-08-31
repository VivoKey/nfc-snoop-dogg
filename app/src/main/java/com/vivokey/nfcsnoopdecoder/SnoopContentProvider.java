package com.vivokey.nfcsnoopdecoder;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class SnoopContentProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    // $(content read --uri content://nfcsnoop)

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        ParcelFileDescriptor[] pipe = null;

        try {
            pipe = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            android.util.Log.e("nfc-snoop-dogg", android.util.Log.getStackTraceString(e));
        }

        try (OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
            String apk = this.getContext().getPackageCodePath();
            String command = "app_process -cp " + apk + " / com.vivokey.nfcsnoopdecoder.NFCSnoopDogg";
            output.write(command.getBytes());
            output.flush();
            output.close();
        } catch (IOException e) {
            android.util.Log.e("nfc-snoop-dogg", android.util.Log.getStackTraceString(e));
        }

        return pipe[0];
    }

    // $(content query --uri content://nfcsnoop | tail -n 1)

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String apk = this.getContext().getPackageCodePath();
        String command = "app_process -cp " + apk + " / com.vivokey.nfcsnoopdecoder.NFCSnoopDogg";
        MatrixCursor cursor = new MatrixCursor(new String[]{""});
        cursor.addRow(new Object[]{"\n" + command});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
