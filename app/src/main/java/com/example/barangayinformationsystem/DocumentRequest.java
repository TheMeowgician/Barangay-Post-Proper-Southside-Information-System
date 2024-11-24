package com.example.barangayinformationsystem;

import com.google.gson.annotations.SerializedName;

public class DocumentRequest {
    @SerializedName("Id")
    private int id;

    @SerializedName("userId")
    private int userId;

    @SerializedName("DocumentType")
    private String documentType;

    @SerializedName("Name")
    private String name;

    @SerializedName("Address")
    private String address;

    @SerializedName("TIN_No")
    private String tinNo;

    @SerializedName("CTC_No")
    private String ctcNo;

    @SerializedName("Alias")
    private String alias;

    @SerializedName("Age")
    private int age;

    @SerializedName("birthday")
    private String birthday;

    @SerializedName("LengthOfStay")
    private int lengthOfStay;

    @SerializedName("Citizenship")
    private String citizenship;

    @SerializedName("Gender")
    private String gender;

    @SerializedName("CivilStatus")
    private String civilStatus;

    @SerializedName("Purpose")
    private String purpose;

    @SerializedName("Status")
    private String status;

    @SerializedName("Quantity")
    private int quantity;

    @SerializedName("DateRequested")
    private String dateRequested;

    @SerializedName("valid_id")
    private String validId;

    @SerializedName("request_picture")
    private String requestPicture;

    @SerializedName("rejection_reason")
    private String rejectionReason;

    // Constructor
    public DocumentRequest() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTinNo() { return tinNo; }
    public void setTinNo(String tinNo) { this.tinNo = tinNo; }

    public String getCtcNo() { return ctcNo; }
    public void setCtcNo(String ctcNo) { this.ctcNo = ctcNo; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public int getLengthOfStay() { return lengthOfStay; }
    public void setLengthOfStay(int lengthOfStay) { this.lengthOfStay = lengthOfStay; }

    public String getCitizenship() { return citizenship; }
    public void setCitizenship(String citizenship) { this.citizenship = citizenship; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getCivilStatus() { return civilStatus; }
    public void setCivilStatus(String civilStatus) { this.civilStatus = civilStatus; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDateRequested() { return dateRequested; }
    public void setDateRequested(String dateRequested) { this.dateRequested = dateRequested; }

    public String getValidId() { return validId; }
    public void setValidId(String validId) { this.validId = validId; }

    public String getRequestPicture() { return requestPicture; }
    public void setRequestPicture(String requestPicture) { this.requestPicture = requestPicture; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}