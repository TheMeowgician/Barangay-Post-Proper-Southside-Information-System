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

        // Direct field access for server-provided address components
        @SerializedName("houseNo")
        private String houseNoField;
        
        @SerializedName("adrZone")
        private String zoneField;
        
        @SerializedName("street")
        private String streetField;
        
        // Improved methods to parse the address with better handling of edge cases
        public String getHouseNo() {
            // First check if the server provided this field directly
            if (houseNoField != null && !houseNoField.isEmpty()) {
                return houseNoField;
            }
            
            if (address == null || address.isEmpty()) {
                return "";
            }
            
            try {
                // Try to extract house number - now handles hyphenated forms like "497-A"
                Pattern pattern = Pattern.compile("^([0-9]+(-[A-Za-z0-9]+)?)\\b");
                Matcher matcher = pattern.matcher(address);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                
                // Fallback: just take the first part if it exists
                String[] parts = address.split("\\s+", 2);
                if (parts.length > 0) {
                    // Check if the first part might be a house number (contains digits)
                    if (parts[0].matches(".*\\d.*")) {
                        return parts[0];
                    }
                }
            } catch (Exception e) {
                Log.e("UserDetailsResponse", "Error parsing house number: " + e.getMessage());
            }
            
            return "";
        }

        public String getZone() {
            // First check if the server provided this field directly
            if (zoneField != null && !zoneField.isEmpty()) {
                return zoneField;
            }
            
            if (address == null || address.isEmpty()) {
                return "";
            }
            
            try {
                // Check for Village name format (like "ISU Village")
                Pattern villagePattern = Pattern.compile("\\b([A-Za-z0-9]+)\\s+Village\\b");
                Matcher villageMatcher = villagePattern.matcher(address);
                if (villageMatcher.find()) {
                    return villageMatcher.group(1) + " Village";
                }
                
                // Pattern to find "Zone X" or "zone X" where X is a number or text
                Pattern pattern = Pattern.compile("\\b[Zz]one\\s+([0-9A-Za-z]+)\\b");
                Matcher matcher = pattern.matcher(address);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                
                // Fallback: look for other identifying parts after street name
                if (address.contains("Street")) {
                    String[] parts = address.split("Street\\s+", 2);
                    if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                        // Return everything after "Street" that's not a Zone indicator
                        String afterStreet = parts[1].trim();
                        if (!afterStreet.toLowerCase().startsWith("zone")) {
                            return afterStreet;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("UserDetailsResponse", "Error parsing zone: " + e.getMessage());
            }
            
            return "";
        }

        public String getStreet() {
            // First check if the server provided this field directly
            if (streetField != null && !streetField.isEmpty()) {
                return streetField;
            }
            
            if (address == null || address.isEmpty()) {
                return "";
            }
            
            try {
                // First extract any content followed by "Street"
                Pattern streetPattern = Pattern.compile("\\b([A-Za-z]+)\\s+Street\\b");
                Matcher streetMatcher = streetPattern.matcher(address);
                if (streetMatcher.find()) {
                    return streetMatcher.group(1);
                }
                
                // If we couldn't find a clear street pattern, try to infer it
                // Skip the house number and look for the first word that's not a number
                String[] addressParts = address.split("\\s+");
                if (addressParts.length > 1) {
                    // Start from the second part (index 1) assuming first part is house number
                    for (int i = 1; i < addressParts.length; i++) {
                        // If this part is a text word (not numeric, not "Street", not "Zone")
                        if (!addressParts[i].matches(".*\\d.*") && 
                            !addressParts[i].equalsIgnoreCase("Street") &&
                            !addressParts[i].equalsIgnoreCase("Zone") &&
                            !addressParts[i].equalsIgnoreCase("Village")) {
                            return addressParts[i];
                        }
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
            // If the server provided the full address, use it directly
            // Force client-side construction to ensure latest parts are used
            // if (address != null && !address.trim().isEmpty()) {
            //    return address.trim();
            // }

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
                // Add "Street" keyword if not already in the address
                // More robust check: add "Street" if the street part itself doesn't end with it or contain it.
                if (!street.toLowerCase().contains("street")) {
                    addressBuilder.append(" Street");
                }
            }
            
            // Add zone/village if available
            String zone = getZone();
            if (!TextUtils.isEmpty(zone)) {
                if (addressBuilder.length() > 0) {
                    addressBuilder.append(" ");
                }
                
                String lowerZone = zone.toLowerCase();
                // Prefix with "Zone " unless it's already there or it's a village type
                if (lowerZone.startsWith("zone ") || lowerZone.contains("village")) {
                    addressBuilder.append(zone);
                } else {
                    addressBuilder.append("Zone ").append(zone);
                }
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