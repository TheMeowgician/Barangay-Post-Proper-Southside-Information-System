package com.example.barangayinformationsystem;

import java.util.ArrayList;
import java.util.List;

public class FAQQuestion {
    private int id;
    private String question;
    private String answer;
    
    public FAQQuestion(int id, String question, String answer) {
        this.id = id;
        this.question = question;
        this.answer = answer;
    }
    
    public int getId() {
        return id;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    // Helper method to generate a list of common FAQs
    public static List<FAQQuestion> getCommonQuestions() {
        List<FAQQuestion> questions = new ArrayList<>();
        
        // Add common questions about the barangay services
        questions.add(new FAQQuestion(1, "How do I request a barangay document?", 
            "To request a barangay document, navigate to the Document Request section " +
            "of the app, select your desired document type, fill out " +
            "the required information, and submit your request. You can track the status " +
            "of your request in the 'Document Status' section."));
        
        questions.add(new FAQQuestion(2, "What are the office hours of Barangay Post Proper Southside?", 
            "Barangay Post Proper Southside office is open from Monday to Friday, 8:00 AM to 5:00 PM, " +
            ". The office is closed on Saturdays, Sundays, and holidays."));
        
        questions.add(new FAQQuestion(3, "How do I report an incident in my area?", 
            "You can report incidents through the 'Incident Report' section of the app. " +
            "Provide a detailed description and add photos or videos if available. Your report " +
            "will be reviewed by the barangay officials."));
        
        questions.add(new FAQQuestion(4, "What documents do I need for barangay documents?", 
            "You will need a valid ID with an address " +
            "and a completed application form which you " +
            "can fill out in the app. For specific requirements, please check the Document " +
            "Request section."));
        
        questions.add(new FAQQuestion(5, "How can I verify if my document request is approved?", 
            "You can check the status of your document requests in the 'Document Status' section " +
            "of the app. The status will update once your document is ready for pickup "));
        
        questions.add(new FAQQuestion(6, "What should I do in case of emergency?", 
            "For emergencies, immediately contact the Barangay Emergency Hotline at 0998-975-0368. " +
            "You can check other hotline numbers on the 'Hotline Numbers' section of the app"));
            
        questions.add(new FAQQuestion(7, "How do I update my information?", 
            "To update your personal information, go to your Profile section which is the icon button on the top right, " +
            "After making changes, press the 'Save Changes' button."));

        questions.add(new FAQQuestion(8, "Where is the Barangay Post Proper Southside Located?", 
            "The Barangay Post Proper Southside is located at Barangay 31 Post Proper Southside Lawton Ave, Taguig City, Metro Manila."));
        return questions;
    }
}