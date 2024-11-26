package com.example.barangayinformationsystem;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class NotificationRecyclerViewItem {

    String nameOfUser;
    String caption;
    int image;

    public NotificationRecyclerViewItem(String nameOfUser, String caption, int image) {
        this.nameOfUser = nameOfUser;
        this.caption = caption;
        this.image = image;
    }

    public String getNameOfUser() {
        return nameOfUser;
    }

    public void setNameOfUser(String nameOfUser) {
        this.nameOfUser = nameOfUser;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }
}
