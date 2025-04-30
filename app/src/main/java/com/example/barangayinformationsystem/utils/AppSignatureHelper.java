package com.example.barangayinformationsystem.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import com.example.barangayinformationsystem.BuildConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class to verify the app's signature
 * This helps ensure the app hasn't been tampered with
 */
public class AppSignatureHelper {
    private static final String TAG = AppSignatureHelper.class.getSimpleName();
    
    /**
     * Get the SHA-1 signature of the application
     * 
     * @param context Application context
     * @return The SHA-1 signature as a Base64 string, or null on error
     */
    public static String getAppSignature(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 
                    PackageManager.GET_SIGNATURES);
            
            if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                return getSHA1(packageInfo.signatures[0]);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting app signature", e);
        }
        
        return null;
    }
    
    /**
     * Calculate SHA-1 hash of a signature
     * 
     * @param signature The signature to hash
     * @return Base64-encoded SHA-1 hash
     */
    private static String getSHA1(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(signature.toByteArray());
            return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error calculating signature hash", e);
        }
        
        return null;
    }
    
    /**
     * Verify if the app signature is valid
     * This helps detect if the app has been repackaged by a third party
     * 
     * @param context Application context
     * @param expectedSignature The expected signature (set this during development)
     * @return true if the signature matches or in debug mode, false otherwise
     */
    public static boolean verifyAppSignature(Context context, String expectedSignature) {
        // In debug builds, don't verify signature to allow development
        if (BuildConfig.DEBUG) {
            return true;
        }
        
        String signature = getAppSignature(context);
        return signature != null && signature.equals(expectedSignature);
    }
}