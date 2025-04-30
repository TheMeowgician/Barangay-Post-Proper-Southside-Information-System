package com.example.barangayinformationsystem.utils;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Utility for verifying digital signatures for server responses
 * This helps ensure data integrity and authenticity
 */
public class SignatureVerifier {
    private static final String TAG = "SignatureVerifier";
    
    // Example public key - in a real app, this would be your server's actual public key
    private static final String SERVER_PUBLIC_KEY = 
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu1SU1LfVLPHCozMxH2Mo"
            + "4lgOEePzNm0tRgeLezV6ffAt0gunVTLw7onLRnrq0/IzW7yWR7QkrmBL7jTKEn5u"
            + "+qKhbwKfBstIs+bMY2Zkp18gnTxKLxoS2tFczGkPLPgizskuemMghRniWaoLcyeh"
            + "kd3qqGElvW/VDL5tby82YzfQ5vh/+n2nUy+gjdkcOTdEcMSHHwxD5ZBfdG3YoPYP"
            + "0FKUH+OqxGYJ1oDn4s0QNuCQCnzvMPGoSZgp4NaGaMUC8VKRKMo3R5o6Z1Bcm9j0"
            + "H1fwqwQClrBVQYIrNpSU+4r4lBQfn0M9B5ch66Im DMOKW8NX8MSLIJjvZp8a"
            + "MwIDAQAB";
    
    /**
     * Verify the digital signature of a data payload
     * 
     * @param data The original data that was signed
     * @param signature Base64-encoded signature to verify
     * @return true if signature is valid, false otherwise
     */
    public static boolean verifySignature(String data, String signature) {
        try {
            // Decode the base64-encoded public key
            byte[] keyBytes = Base64.decode(SERVER_PUBLIC_KEY, Base64.DEFAULT);
            
            // Generate a public key
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            // Create a Signature object for verification
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            
            // Add data
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            
            // Verify
            byte[] signatureBytes = Base64.decode(signature, Base64.DEFAULT);
            return sig.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
            Log.e(TAG, "Error verifying signature", e);
            return false;
        }
    }
}