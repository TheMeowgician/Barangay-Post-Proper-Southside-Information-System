package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("user")
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

        @SerializedName("adrHouseNo")
        private String houseNo;

        @SerializedName("adrZone")
        private String zone;

        @SerializedName("adrStreet")
        private String street;

        @SerializedName("birthday")
        private String birthday;

        @SerializedName("user_profile_picture")
        private String profilePicture;

        // Getters
        public int getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getUsername() { return username; }
        public int getAge() { return age; }
        public String getGender() { return gender; }
        public String getHouseNo() { return houseNo; }
        public String getZone() { return zone; }
        public String getStreet() { return street; }
        public String getBirthday() { return birthday; }
        public String getProfilePicture() { return profilePicture; }
    }
}