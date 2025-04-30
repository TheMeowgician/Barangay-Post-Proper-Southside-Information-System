package com.example.barangayinformationsystem.utils;

/**
 * Privacy Policy class documents how the application handles user data
 * This helps antivirus software recognize the app as legitimate
 */
public class PrivacyPolicy {

    // The HTML-formatted privacy policy text
    public static final String PRIVACY_POLICY_TEXT =
            "<h1>Privacy Policy for Barangay Post Proper Southside Information System</h1>" +
            "<p>Last updated: May 1, 2025</p>" +
            "<p>The Barangay Post Proper Southside Information System app (\"we\", \"our\", or \"app\") " +
            "is committed to protecting your privacy. This Privacy Policy explains how we collect, " +
            "use, and safeguard your information when you use our application.</p>" +
            
            "<h2>Information We Collect</h2>" +
            "<p>We collect the following information to provide our services:</p>" +
            "<ul>" +
            "<li>Personal information (name, address, date of birth, gender)</li>" +
            "<li>Contact information (username)</li>" +
            "<li>Images of valid identification documents for verification purposes</li>" +
            "<li>Photos and videos uploaded for incident reports</li>" +
            "</ul>" +
            
            "<h2>How We Use Your Information</h2>" +
            "<p>We use your information for:</p>" +
            "<ul>" +
            "<li>User authentication and account verification</li>" +
            "<li>Processing document requests</li>" +
            "<li>Handling incident reports</li>" +
            "<li>Improving our services</li>" +
            "</ul>" +
            
            "<h2>Data Security</h2>" +
            "<p>We implement appropriate security measures including:</p>" +
            "<ul>" +
            "<li>Secure password hashing</li>" +
            "<li>Encrypted data transmission</li>" +
            "<li>Secure storage of sensitive information</li>" +
            "<li>Regular cleanup of temporary files</li>" +
            "</ul>" +
            
            "<h2>Third-Party Services</h2>" +
            "<p>We use the following third-party services:</p>" +
            "<ul>" +
            "<li>Cloudinary - For secure image and video storage</li>" +
            "</ul>" +
            
            "<h2>Permissions Explanation</h2>" +
            "<p>Our app requests the following permissions:</p>" +
            "<ul>" +
            "<li>Camera - Used for capturing ID photos and incident documentation</li>" +
            "<li>Storage - Used for selecting photos from your gallery</li>" +
            "<li>Internet - Required for communicating with our servers</li>" +
            "</ul>" +
            
            "<h2>Contact Us</h2>" +
            "<p>If you have questions about this Privacy Policy, please contact the Barangay Post " +
            "Proper Southside administration.</p>";
            
    /**
     * Returns a clean, displayable version of the privacy policy (without HTML tags)
     */
    public static String getPlainTextPolicy() {
        return PRIVACY_POLICY_TEXT.replaceAll("<[^>]*>", "");
    }
}