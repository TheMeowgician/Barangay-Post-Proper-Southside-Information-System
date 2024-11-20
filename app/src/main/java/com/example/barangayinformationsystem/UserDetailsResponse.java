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
        private String username; // VARCHAR
        private String firstName; // VARCHAR
        private String lastName; // VARCHAR
        private String address; // VARCHAR
        private int age; // int in DB
        private String gender; // VARCHAR
        private String dateOfBirth; // VARCHAR or DATE
        private String password; // VARCHAR

        public String getUsername() { return username; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getAddress() { return address; }
        public int getAge() { return age; }
        public String getGender() { return gender; }
        public String getDateOfBirth() { return dateOfBirth; }
        public String getPassword() { return password; }
    }
}
