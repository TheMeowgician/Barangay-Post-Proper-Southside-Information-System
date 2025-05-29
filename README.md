# Barangay Information System

This is an Android application designed to serve as an information system for a barangay (the smallest administrative division in the Philippines).

## Features
*   User authentication
*   Resident information management
*   Viewing and potentially uploading documents/images (likely using Cloudinary)
*   QR code scanning (possibly for resident identification or document verification)
*   Displaying announcements or news
*   Crash reporting via Firebase Crashlytics

## Technologies Used

*   **Programming Language:** Java/Kotlin (assumed, standard for Android)
*   **Android SDK:** Minimum API level 23, Target API level 34
*   **User Interface:**
    *   Material Design Components
    *   ConstraintLayout
    *   CardView
    *   PhotoView (for zoomable images)
    *   CircleImageView
    *   SwipeRefreshLayout (for pull-to-refresh functionality)
    *   JustifyText (for text justification)
*   **Networking:**
    *   Retrofit (for making API calls)
    *   OkHttp Logging Interceptor (for network request/response logging)
    *   Gson (for JSON parsing)
*   **Image Handling:**
    *   Glide (for efficient image loading and caching)
    *   Cloudinary (for cloud-based image and video management)
*   **QR Code Scanning:**
    *   ZXing Android Embedded
*   **Crash Reporting:**
    *   Firebase Crashlytics
*   **Build System:** Gradle

## Setup and Installation
1.  Clone the repository: `git clone <repository-url>`
2.  Open the project in Android Studio.
3.  Ensure you have the Android SDK for API level 34 installed.
4.  The project uses API keys for Cloudinary
5.  Build and run the application on an Android emulator or a physical device.


