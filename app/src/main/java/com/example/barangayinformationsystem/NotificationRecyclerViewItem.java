package com.example.barangayinformationsystem;

public class NotificationRecyclerViewItem {
    private String nameOfUser;
    private String caption;
    private int image;

    public NotificationRecyclerViewItem(String nameOfUser, String caption, int image) {
        this.nameOfUser = nameOfUser;
        this.caption = caption;
        this.image = image;
    }

    public NotificationRecyclerViewItem(String caption) {
        this.nameOfUser = "Post Proper Southside"; // Fixed value
        this.caption = caption;
        this.image = R.drawable.notification_pps_logo; // Fixed value
    }

    public String getNameOfUser() {
        return nameOfUser;
    }

    public String getCaption() {
        return caption;
    }

    public int getImage() {
        return image;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NotificationRecyclerViewItem that = (NotificationRecyclerViewItem) obj;
        return caption.equals(that.caption); // Compare by title to prevent duplicates
    }

    @Override
    public int hashCode() {
        return caption.hashCode();
    }
}
