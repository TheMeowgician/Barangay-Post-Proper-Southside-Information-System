package com.example.barangayinformationsystem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUploadUtil {

    public static File prepareImageForUpload(Context context, Uri imageUri, String prefix) throws IOException {
        // Create a temporary file
        File outputDir = context.getCacheDir();
        File outputFile = File.createTempFile(prefix + "_", ".jpg", outputDir);

        // Get input stream from Uri
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
        if (inputStream != null) inputStream.close();

        // Resize image if needed
        Bitmap resizedBitmap = resizeImageIfNeeded(originalBitmap, 1024, 1024);

        // Compress and save
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);

        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(bos.toByteArray());
        fos.close();

        // Clean up
        if (resizedBitmap != originalBitmap) {
            originalBitmap.recycle();
        }
        resizedBitmap.recycle();

        return outputFile;
    }

    private static Bitmap resizeImageIfNeeded(Bitmap image, int maxWidth, int maxHeight) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return image;
        }

        float ratio = Math.min(
                (float) maxWidth / width,
                (float) maxHeight / height
        );

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(image, newWidth, newHeight, true);
    }
}