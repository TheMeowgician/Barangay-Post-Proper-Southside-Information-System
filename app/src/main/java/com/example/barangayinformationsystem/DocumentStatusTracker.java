package com.example.barangayinformationsystem;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class DocumentStatusTracker {
    private static DocumentStatusTracker instance;
    private Map<Integer, String> documentStatusMap = new HashMap<>();
    private static final String PREF_KEY_TRACKED_DOCUMENTS = "tracked_document_statuses";

    private DocumentStatusTracker() {
        // Private constructor for singleton
    }

    public static synchronized DocumentStatusTracker getInstance() {
        if (instance == null) {
            instance = new DocumentStatusTracker();
        }
        return instance;
    }

    // Returns true if status has changed
    public boolean hasStatusChanged(int documentId, String currentStatus) {
        String previousStatus = documentStatusMap.get(documentId);

        // If we have no previous status or status has changed
        if (previousStatus == null || !previousStatus.equals(currentStatus)) {
            documentStatusMap.put(documentId, currentStatus);
            return previousStatus != null; // Only return true if we had a previous status (not first time seeing)
        }
        return false;
    }

    // Save tracked statuses to preferences
    public void saveTrackedStatuses(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> statusSet = new HashSet<>();
        for (Map.Entry<Integer, String> entry : documentStatusMap.entrySet()) {
            statusSet.add(entry.getKey() + "|" + entry.getValue());
        }

        editor.putStringSet(PREF_KEY_TRACKED_DOCUMENTS, statusSet);
        editor.apply();
    }

    // Load tracked statuses from preferences
    public void loadTrackedStatuses(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> statusSet = prefs.getStringSet(PREF_KEY_TRACKED_DOCUMENTS, new HashSet<String>());

        documentStatusMap.clear();
        for (String statusEntry : statusSet) {
            String[] parts = statusEntry.split("\\|");
            if (parts.length == 2) {
                try {
                    int docId = Integer.parseInt(parts[0]);
                    documentStatusMap.put(docId, parts[1]);
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
    }    // Get the tracked status for a document
    public String getTrackedStatus(int documentId) {
        return documentStatusMap.get(documentId);
    }

    // Update the status for a document
    public void updateStatus(int documentId, String newStatus) {
        documentStatusMap.put(documentId, newStatus);
    }

    // Clear all tracked statuses
    public void clear() {
        documentStatusMap.clear();
    }
}