package com.example.barangayinformationsystem;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class IncidentStatusTracker {
    private static IncidentStatusTracker instance;
    private Map<Integer, String> incidentStatusMap = new HashMap<>();
    private static final String PREF_KEY_TRACKED_INCIDENTS = "tracked_incident_statuses";

    private IncidentStatusTracker() {
        // Private constructor for singleton
    }

    public static synchronized IncidentStatusTracker getInstance() {
        if (instance == null) {
            instance = new IncidentStatusTracker();
        }
        return instance;
    }

    // Returns true if incident status has changed
    public boolean hasIncidentStatusChanged(int incidentId, String currentStatus) {
        String previousStatus = incidentStatusMap.get(incidentId);

        // If we have no previous status or status has changed
        if (previousStatus == null || !previousStatus.equals(currentStatus)) {
            incidentStatusMap.put(incidentId, currentStatus);
            return previousStatus != null; // Only return true if we had a previous status (not first time seeing)
        }
        return false;
    }

    // Get the tracked status for an incident
    public String getTrackedIncidentStatus(int incidentId) {
        return incidentStatusMap.get(incidentId);
    }

    // Update the status for an incident
    public void updateIncidentStatus(int incidentId, String newStatus) {
        incidentStatusMap.put(incidentId, newStatus);
    }

    // Save tracked statuses to preferences
    public void saveTrackedStatuses(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> incidentStatusSet = new HashSet<>();
        for (Map.Entry<Integer, String> entry : incidentStatusMap.entrySet()) {
            incidentStatusSet.add(entry.getKey() + "|" + entry.getValue());
        }
        editor.putStringSet(PREF_KEY_TRACKED_INCIDENTS, incidentStatusSet);

        editor.apply();
    }

    // Load tracked statuses from preferences
    public void loadTrackedStatuses(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Load incident statuses
        Set<String> incidentStatusSet = prefs.getStringSet(PREF_KEY_TRACKED_INCIDENTS, new HashSet<String>());
        incidentStatusMap.clear();
        for (String statusEntry : incidentStatusSet) {
            String[] parts = statusEntry.split("\\|");
            if (parts.length == 2) {
                try {
                    int incidentId = Integer.parseInt(parts[0]);
                    incidentStatusMap.put(incidentId, parts[1]);
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
    }

    // Clear all tracked statuses
    public void clear() {
        incidentStatusMap.clear();
    }
} 