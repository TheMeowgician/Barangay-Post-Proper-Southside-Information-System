package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnouncementResponse that = (AnnouncementResponse) o;
        return id == that.id &&
                Objects.equals(announcementTitle, that.announcementTitle) &&
                Objects.equals(descriptionText, that.descriptionText) &&
                Objects.equals(announcementImages, that.announcementImages) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(postedAt, that.postedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, announcementTitle, descriptionText,
                announcementImages, createdAt, postedAt);
    }
}