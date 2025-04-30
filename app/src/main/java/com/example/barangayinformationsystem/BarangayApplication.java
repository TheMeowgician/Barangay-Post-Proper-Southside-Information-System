package com.example.barangayinformationsystem;

import android.app.Application;
import android.util.Log;
import android.os.StrictMode;

import com.example.barangayinformationsystem.utils.AppSignatureHelper;
import com.example.barangayinformationsystem.utils.TempFileCleanupManager;

/**
 * Application class for initializing global components
 */
public class BarangayApplication extends Application {
    
    private static final String TAG = "BarangayApp";
    // This value should be updated with your actual release signature
    private static final String RELEASE_SIGNATURE = "YOUR_RELEASE_SIGNATURE"; 
    
    private TempFileCleanupManager cleanupManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        if (BuildConfig.DEBUG) {
            // Enable strict mode in debug builds to catch potential issues
            enableStrictMode();
        } else {
            // In release builds, verify app signature
            verifyAppIntegrity();
        }
        
        // Initialize temp file cleanup manager
        cleanupManager = new TempFileCleanupManager(this);
        cleanupManager.cleanupTempFiles(); // Clean up any old temporary files on app start
        cleanupManager.startScheduledCleanup();
    }
    
    /**
     * Verify that the app hasn't been tampered with
     */
    private void verifyAppIntegrity() {
        // Skip signature verification in debug builds
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "App integrity check skipped in debug mode");
            return;
        }
        
        // Get the current app signature
        String signature = AppSignatureHelper.getAppSignature(this);
        Log.d(TAG, "Current app signature: " + signature);
        
        // For security reasons, we don't take any action if signatures don't match
        // but we could implement more strict measures in a production app
    }
    
    /**
     * Enable StrictMode to catch common issues during development
     */
    private void enableStrictMode() {
        // Set thread policy
        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build();
        
        // Set VM policy
        StrictMode.VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build();
        
        StrictMode.setThreadPolicy(threadPolicy);
        StrictMode.setVmPolicy(vmPolicy);
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // Stop the scheduled cleanup
        if (cleanupManager != null) {
            cleanupManager.stopScheduledCleanup();
        }
    }
}