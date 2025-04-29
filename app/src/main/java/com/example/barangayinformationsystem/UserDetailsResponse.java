package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;

import android.text.TextUtils;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserDetailsResponse {
    private String status;
    private String message;
    private User user;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public static class User {
        @SerializedName("id")
        private int id;

        @SerializedName("firstName")
        private String firstName;

        @SerializedName("lastName")
        private String lastName;

        @SerializedName("username")
        private String username;

        @SerializedName("age")
        private int age;

        @SerializedName("gender")
        private String gender;

        @SerializedName("address")  // Changed from separate fields to match PHP
        private String address;

        @SerializedName("dateOfBirth")  // Changed to match PHP
        private String dateOfBirth;

        @SerializedName("password")
        private String password;

        @SerializedName("profilePicture")  // Changed to match PHP
        private String profilePicture;

        // Improved methods to parse the address with better handling of edge cases
        public String getHouseNo() {
            if (address == null || address.isEmpty()) {
                return "";
            }
            
            try {
                // Try to extract house number - typically the first numeric part of the address
                Pattern pattern = Pattern.compile("^([0-9]+[a-zA-Z]?)\\b");
                Matcher matcher = pattern.matcher(address);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                
                // Fallback: just take the first part if it exists
                String[] parts = address.split("\\s+", 2);
                if (parts.length > 0) {
                    return parts[0];
                }
            } catch (Exception e) {
                Log.e("UserDetailsResponse", "Error parsing house number: " + e.getMessage());
            }
            
            return "";
        }

        public String getZone() {
            if (address == null || address.isEmpty()) {
                return "";
            }
            
            try {
                // Pattern to find "Zone X" or "zone X" where X is a number, possibly followed by other text
                Pattern pattern = Pattern.compile("\\b[Zz]one\\s+([0-9]+)\\b");
                Matcher matcher = pattern.matcher(address);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                
                // Fallback: look for zone at the end of address
                if (address.toLowerCase().contains("zone")) {
                    String[] parts = address.split("\\s+[Zz]one\\s+");
                    if (parts.length > 1) {
                        // Get the first word after "Zone"
                        return parts[1].split("\\s+")[0];
                    }
                }
            } catch (Exception e) {
                Log.e("UserDetailsResponse", "Error parsing zone: " + e.getMessage());
            }
            
            return "";
        }

        public String getStreet() {
            if (address == null || address.isEmpty()) {
                return "";
            }
            
            try {
                // Pattern to capture multi-word street names before the word "Street"
                // This looks for 1+ words (not containing "Zone") followed by "Street"
                Pattern pattern = Pattern.compile("\\b((?:[^\\s]+\\s+)(?!Zone|zone)[^\\s]+)\\s+[Ss]treet\\b");
                Matcher matcher = pattern.matcher(address);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                
                // Simpler approach looking for the word before "Street"
                if (address.contains("Street") || address.contains("street")) {
                    Pattern simplePattern = Pattern.compile("\\b([^\\s]+)\\s+[Ss]treet\\b");
                    Matcher simpleMatcher = simplePattern.matcher(address);
                    if (simpleMatcher.find()) {
                        return simpleMatcher.group(1);
                    }
                }
                
                // Fallback - if address has a structure like "123 Main Street Zone 4"
                // Extract the part between the first word and "Zone" or end of string
                String[] parts = address.split("\\s+", 2);
                if (parts.length > 1) {
                    String afterHouseNumber = parts[1];
                    String[] streetParts = afterHouseNumber.split("\\s+[Zz]one\\s+");
                    if (streetParts.length > 0) {
                        // Remove "Street" keyword if present
                        return streetParts[0].replace("Street", "").replace("street", "").trim();
                    }
                }
            } catch (Exception e) {
                Log.e("UserDetailsResponse", "Error parsing street: " + e.getMessage());
            }
            
            return "";
        }

        /**
         * Get a properly formatted complete address as a single string
         * @return Formatted address string
         */
        public String getFormattedAddress() {
            if (address != null && !address.trim().isEmpty()) {
                // Just return the original address if it's already provided
                return address.trim();
            }

            StringBuilder addressBuilder = new StringBuilder();
            
            // Add house number if available
            String houseNo = getHouseNo();
            if (!TextUtils.isEmpty(houseNo)) {
                addressBuilder.append(houseNo);
            }
            
            // Add street if available
            String street = getStreet();
            if (!TextUtils.isEmpty(street)) {
                if (addressBuilder.length() > 0) {
                    addressBuilder.append(" ");
                }
                addressBuilder.append(street);
                // Add "Street" keyword if not already in the street name
                if (!street.toLowerCase().contains("street")) {
                    addressBuilder.append(" Street");
                }
            }
            
            // Add zone if available
            String zone = getZone();
            if (!TextUtils.isEmpty(zone)) {
                if (addressBuilder.length() > 0) {
                    addressBuilder.append(" ");
                }
                addressBuilder.append("Zone ").append(zone);
            }
            
            return addressBuilder.toString();
        }

        // Getters
        public int getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getUsername() { return username; }
        public int getAge() { return age; }
        public String getGender() { return gender; }
        public String getAddress() { return address; }
        public String getDateOfBirth() { return dateOfBirth; }
        public String getPassword() { return password; }
        public String getProfilePicture() { return profilePicture; }
    }
}