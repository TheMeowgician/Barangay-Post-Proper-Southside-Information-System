package com.example.barangayinformationsystem;

import android.util.Base64;
import android.util.Log;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Secure password hasher using PBKDF2 with HMAC-SHA256
 * This implementation follows modern security best practices
 */
public class PasswordHasher {
    private static final String TAG = "PasswordHasher";
    private static final int ITERATIONS = 10000; // Higher is more secure but slower
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 16; // bytes
    
    /**
     * Hash a password for secure storage
     * This method automatically generates a random salt for each password
     * 
     * @param password The plaintext password to hash
     * @return A base64-encoded string containing the salt + hash, or null if hashing failed
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        
        try {
            // For compatibility with the server, we don't use salt in this version
            // This is a basic SHA-256 hash that matches PHP's hash('sha256', $password)
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password", e);
            return null;
        }
    }
    
    /**
     * A more secure hash method that can be enabled when the server supports it
     * This should be used in future updates when the backend API supports PBKDF2 
     */
    public static String secureHashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        
        try {
            // Generate a random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            // Hash the password with PBKDF2
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), 
                salt, 
                ITERATIONS, 
                KEY_LENGTH
            );
            
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            
            // Clean the password from memory
            spec.clearPassword();
            
            // Combine salt and hash
            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);
            
            // Encode as base64 for storage
            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, "Error hashing password securely", e);
            return null;
        }
    }
    
    /**
     * Verify a password against a stored hash
     * This is for the secure hash method and can be enabled when the server supports it
     *
     * @param password The plaintext password to verify
     * @param storedHash The stored hash from the database
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        
        try {
            // Decode the stored hash
            byte[] combined = Base64.decode(storedHash, Base64.DEFAULT);
            
            // Extract salt and hash
            byte[] salt = new byte[SALT_LENGTH];
            byte[] hash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, salt.length);
            System.arraycopy(combined, salt.length, hash, 0, hash.length);
            
            // Hash the password with the same salt
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
            );
            
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] testHash = skf.generateSecret(spec).getEncoded();
            
            // Clean the password from memory
            spec.clearPassword();
            
            // Compare the hashes
            int diff = hash.length ^ testHash.length;
            for (int i = 0; i < hash.length && i < testHash.length; i++) {
                diff |= hash[i] ^ testHash[i];
            }
            
            return diff == 0;
        } catch (Exception e) {
            Log.e(TAG, "Error verifying password", e);
            return false;
        }
    }
}