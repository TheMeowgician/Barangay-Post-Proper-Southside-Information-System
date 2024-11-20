package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AnnouncementResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("announcement_title")
    private String announcementTitle;

    @SerializedName("description_text")
    private String descriptionText;

    @SerializedName("announcement_images")
    private List<String> announcementImages;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("posted_at")
    private String postedAt;

    // Getters
    public int getId() { return id; }
    public String getAnnouncementTitle() { return announcementTitle; }
    public String getDescriptionText() { return descriptionText; }
    public List<String> getAnnouncementImages() { return announcementImages; }
    public String getCreatedAt() { return createdAt; }
    public String getPostedAt() { return postedAt; }
}