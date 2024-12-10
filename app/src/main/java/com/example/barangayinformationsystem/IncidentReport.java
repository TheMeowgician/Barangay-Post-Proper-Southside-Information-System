package com.example.barangayinformationsystem;

public class IncidentReport {
    private int id;
    private String name;
    private String title;
    private String description;
    private String incident_picture;
    private String date_submitted;
    private String status;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIncidentPicture() { return incident_picture; }
    public void setIncidentPicture(String incident_picture) { this.incident_picture = incident_picture; }

    public String getDateSubmitted() { return date_submitted; }
    public void setDateSubmitted(String date_submitted) { this.date_submitted = date_submitted; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
