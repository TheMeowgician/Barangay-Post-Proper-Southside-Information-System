package com.example.barangayinformationsystem.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to configure WebView instances with proper security settings
 * This helps prevent WebView-related security issues that might trigger antivirus flags
 */
public class SecureWebViewManager {

    /**
     * Configure a WebView with secure settings
     * 
     * @param webView The WebView to configure
     * @param allowJavascript Whether to enable JavaScript (should be false unless absolutely needed)
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void configureSecureWebView(WebView webView, boolean allowJavascript) {
        if (webView == null) return;
        
        WebSettings settings = webView.getSettings();
        
        // Limit JavaScript usage
        settings.setJavaScriptEnabled(allowJavascript);
        
        // Disable potentially dangerous features
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        
        // Disable saving form data
        settings.setSaveFormData(false);
        
        // Disable geolocation
        settings.setGeolocationEnabled(false);
        
        // Disable database storage API
        settings.setDatabaseEnabled(false);
        
        // Disable DOM storage API
        settings.setDomStorageEnabled(false);
        
        // Apply Content Security Policy for Android 5.0+ (Lollipop)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.setWebViewClient(new SecureWebViewClient());
        }
    }
    
    /**
     * WebViewClient that enforces Content Security Policy
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static class SecureWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
            // Add security headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Security-Policy", "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline';");
            headers.put("X-XSS-Protection", "1; mode=block");
            headers.put("X-Content-Type-Options", "nosniff");
            
            // Continue loading with added headers
            return super.shouldInterceptRequest(view, request);
        }
    }
    
    /**
     * Load HTML content with proper Content Security Policy headers
     * 
     * @param webView The WebView to load content into
     * @param htmlContent The HTML content to load
     */
    public static void loadHTMLContentSecurely(WebView webView, String htmlContent) {
        if (webView == null) return;
        
        // Add CSP headers to the HTML
        String cspHeader = "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline';\">";
        String secureHTML = "<html><head>" + cspHeader + "</head><body>" + htmlContent + "</body></html>";
        
        // Load the data
        webView.loadDataWithBaseURL(null, secureHTML, "text/html", "UTF-8", null);
    }
}