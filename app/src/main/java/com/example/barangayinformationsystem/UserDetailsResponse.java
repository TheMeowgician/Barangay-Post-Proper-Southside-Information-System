package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;

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

        // Add these methods to parse the address
        public String getHouseNo() {
            if (address != null) {
                String[] parts = address.split(" ");
                return parts.length > 0 ? parts[0] : "";
            }
            return "";
        }

        public String getZone() {
            if (address != null && address.contains("Zone")) {
                String[] parts = address.split(" ");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals("Zone")) {
                        return parts[i + 1];
                    }
                }
            }
            return "";
        }

        public String getStreet() {
            if (address != null && address.contains("Street")) {
                String[] parts = address.split(" ");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i + 1].equals("Street")) {
                        return parts[i];
                    }
                }
            }
            return "";
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