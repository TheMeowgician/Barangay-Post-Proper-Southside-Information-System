package com.example.barangayinformationsystem.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A secure download manager that validates files before saving them
 * This helps avoid downloading and storing potentially harmful files
 */
public class SafeDownloadManager {
    private static final String TAG = "SafeDownloadManager";
    private static final int BUFFER_SIZE = 4096;
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    
    // Allowed MIME types for download
    private static final String[] ALLOWED_MIME_TYPES = {
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // DOCX
    };
    
    /**
     * Downloads a file asynchronously with security validation
     * 
     * @param context Application context
     * @param fileUrl URL of the file to download
     * @param expectedSha256 Expected SHA-256 hash of the file (can be null if unknown)
     * @param listener Callback for download status updates
     */
    public static void downloadFile(
            final Context context, 
            final String fileUrl, 
            final String expectedSha256,
            final DownloadListener listener) {
        
        executor.execute(() -> {
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            File outputFile = null;
            
            try {
                URL url = new URL(fileUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    notifyError(listener, "Server returned HTTP " + connection.getResponseCode());
                    return;
                }
                
                // Get content type and validate MIME type
                String contentType = connection.getContentType();
                if (!isAllowedMimeType(contentType)) {
                    notifyError(listener, "File type not allowed: " + contentType);
                    return;
                }
                
                // Get file name from URL
                String fileName = getFileNameFromUrl(fileUrl);
                
                // Create output file in Downloads directory
                File downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadsDir == null) {
                    notifyError(listener, "Cannot access downloads directory");
                    return;
                }
                
                // Create temp file with .tmp extension
                outputFile = new File(downloadsDir, fileName + ".tmp");
                
                // Get file length
                int fileLength = connection.getContentLength();
                
                // Download the file
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(outputFile);
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                int totalBytesRead = 0;
                MessageDigest digest = null;
                
                // Initialize SHA-256 for integrity check
                if (expectedSha256 != null) {
                    try {
                        digest = MessageDigest.getInstance("SHA-256");
                    } catch (NoSuchAlgorithmException e) {
                        Log.e(TAG, "SHA-256 not supported", e);
                    }
                }
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    
                    // Update hash if we're verifying
                    if (digest != null) {
                        digest.update(buffer, 0, bytesRead);
                    }
                    
                    // Update progress
                    totalBytesRead += bytesRead;
                    if (fileLength > 0) {
                        final int progress = (int) (totalBytesRead * 100 / fileLength);
                        notifyProgress(listener, progress);
                    }
                }
                
                // Close streams
                outputStream.flush();
                outputStream.close();
                outputStream = null;
                inputStream.close();
                inputStream = null;
                
                // Verify hash if expected hash was provided
                if (expectedSha256 != null && digest != null) {
                    byte[] hashBytes = digest.digest();
                    String fileHash = bytesToHex(hashBytes);
                    
                    if (!fileHash.equalsIgnoreCase(expectedSha256)) {
                        if (outputFile != null && outputFile.exists()) {
                            outputFile.delete();
                        }
                        notifyError(listener, "File integrity check failed");
                        return;
                    }
                }
                
                // Rename from .tmp to actual filename
                File finalFile = new File(downloadsDir, fileName);
                if (outputFile.renameTo(finalFile)) {
                    notifySuccess(listener, finalFile.getAbsolutePath());
                } else {
                    notifyError(listener, "Failed to finalize download");
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Download error", e);
                notifyError(listener, "Download error: " + e.getMessage());
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
    
    /**
     * Check if the MIME type is allowed
     */
    private static boolean isAllowedMimeType(String mimeType) {
        if (mimeType == null) return false;
        
        for (String allowed : ALLOWED_MIME_TYPES) {
            if (mimeType.equals(allowed)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get file name from URL
     */
    private static String getFileNameFromUrl(String url) {
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        // Remove query parameters if any
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf('?'));
        }
        return fileName;
    }
    
    /**
     * Convert bytes to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private static void notifyProgress(final DownloadListener listener, final int progress) {
        if (listener != null) {
            listener.onProgressUpdate(progress);
        }
    }
    
    private static void notifySuccess(final DownloadListener listener, final String filePath) {
        if (listener != null) {
            listener.onDownloadComplete(filePath);
        }
    }
    
    private static void notifyError(final DownloadListener listener, final String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }
    
    /**
     * Listener for download events
     */
    public interface DownloadListener {
        void onProgressUpdate(int progress);
        void onDownloadComplete(String filePath);
        void onError(String error);
    }
}