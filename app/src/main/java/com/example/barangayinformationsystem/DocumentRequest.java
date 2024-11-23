package com.example.barangayinformationsystem;

public class DocumentRequest {
    private int id;
    private String documentType;
    private String name;
    private String address;
    private String tinNo;
    private String ctcNo;
    private String alias;
    private int age;
    private int lengthOfStay;
    private String citizenship;
    private String gender;
    private String civilStatus;
    private String purpose;
    private String status;
    private int quantity;
    private String dateRequested;
    private String validId;
    private String requestPicture;
    private String rejectionReason;

    // Constructor
    public DocumentRequest() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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