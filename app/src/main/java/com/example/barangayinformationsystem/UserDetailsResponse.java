package com.example.barangayinformationsystem;

public class UserDetailsResponse {
    private String status;
    private User user;

    public String getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }

    public static class User {
        private String username;
        private String firstName;
        private String lastName;
        private String address;
        private int age;
        private String gender;
        private String dateOfBirth;
        private String password;
        private String userProfilePicture;  // Make sure this is defined

        public String getUsername() {
            return username;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getAddress() {
            return address;
        }

        public int getAge() {
            return age;
        }

        public String getGender() {
            return gender;
        }

        public String getDateOfBirth() {
            return dateOfBirth;
        }

        public String getPassword() {
            return password;
        }

        public String getUserProfilePicture() {
            return userProfilePicture;  // Make sure this matches your response field
        }
    }
}
