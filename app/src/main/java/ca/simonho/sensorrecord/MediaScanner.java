package ca.simonho.sensorrecord;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

public class MediaScanner {
    private final String TAG = "MediaScanner";

    protected void scanFile(final Context context, String[] files, String[] mimeTypes, final Logger logger) {
        MediaScannerConnection.scanFile(context, files, mimeTypes,
            new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {

                    if (uri == null) {
                        logger.e(context, TAG, "Media scan failed");
                        logger.e(context, TAG, "Failed path: " + path);
                    } else {
                        logger.v(context, TAG, "Scan successful: " + path);
                        logger.v(context, TAG, "Succeed uri: " + uri);
                    }
                }
            }
        );
    }
}