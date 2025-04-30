package com.example.barangayinformationsystem.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to manage and clean up temporary files created by the application
 * Helps prevent accumulating large files in cache that might trigger security warnings
 */
public class TempFileCleanupManager {
    private static final String TAG = "TempFileCleanup";
    private static final long MAX_CACHE_AGE_HOURS = 24; // Files older than 24 hours will be deleted
    private static final long CLEANUP_INTERVAL_HOURS = 12; // Clean up every 12 hours
    
    private final Context context;
    private final ScheduledExecutorService scheduler;

    public TempFileCleanupManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    /**
     * Start the scheduled cleanup of temporary files
     */
    public void startScheduledCleanup() {
        scheduler.scheduleAtFixedRate(
            this::cleanupTempFiles,
            1, // Start after 1 hour
            CLEANUP_INTERVAL_HOURS,
            TimeUnit.HOURS
        );
    }
    
    /**
     * Clean up temporary files now
     */
    public void cleanupTempFiles() {
        try {
            File cacheDir = context.getCacheDir();
            File externalCacheDir = context.getExternalCacheDir();
            
            long cutoffTime = System.currentTimeMillis() - (MAX_CACHE_AGE_HOURS * 60 * 60 * 1000);
            
            int deletedCount = 0;
            // Clean internal cache
            if (cacheDir != null && cacheDir.exists()) {
                deletedCount += cleanDirectory(cacheDir, cutoffTime);
            }
            
            // Clean external cache if available
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deletedCount += cleanDirectory(externalCacheDir, cutoffTime);
            }
            
            // Log on main thread to avoid threading issues
            final int finalDeletedCount = deletedCount;
            new Handler(Looper.getMainLooper()).post(() -> 
                Log.d(TAG, "Cleanup complete: " + finalDeletedCount + " temporary files removed"));
        } catch (Exception e) {
            Log.e(TAG, "Error during temp file cleanup", e);
        }
    }
    
    /**
     * Clean all files in directory that are older than the cutoff time
     * @param directory Directory to clean
     * @param cutoffTime Timestamp before which files will be deleted
     * @return Number of files deleted
     */
    private int cleanDirectory(File directory, long cutoffTime) {
        int deletedCount = 0;
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    // Check if file is older than cutoff time and matches our temp file patterns
                    if (file.lastModified() < cutoffTime && 
                        (file.getName().startsWith("temp_video_") || 
                         file.getName().startsWith("upload_video") ||
                         file.getName().startsWith("video_to_upload_") ||
                         file.getName().startsWith("recorded_video") ||
                         file.getName().startsWith("valid_id_"))) {
                        
                        boolean deleted = file.delete();
                        if (deleted) {
                            deletedCount++;
                        }
                    }
                } else if (file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..")) {
                    // Recursively clean subdirectories
                    deletedCount += cleanDirectory(file, cutoffTime);
                }
            }
        }
        
        return deletedCount;
    }
    
    /**
     * Stop the scheduled cleanup
     */
    public void stopScheduledCleanup() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}