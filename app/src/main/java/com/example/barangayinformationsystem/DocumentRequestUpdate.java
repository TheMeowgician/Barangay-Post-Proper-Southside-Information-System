package com.example.barangayinformationsystem;

public class DocumentRequestUpdate {

    private int Id;
    private String DocumentType;
    private String Status;
    private String DateRequested;

    public DocumentRequestUpdate(int id, String documentType, String status, String dateRequested) {
        Id = id;
        DocumentType = documentType;
        Status = status;
        DateRequested = dateRequested;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getDocumentType() {
        return DocumentType;
    }

    public void setDocumentType(String documentType) {
        DocumentType = documentType;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public String getDateRequested() {
        return DateRequested;
    }

    public void setDateRequested(String dateRequested) {
        DateRequested = dateRequested;
    }
}
