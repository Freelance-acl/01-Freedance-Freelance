package com.team01.freelance.proposal.dto;

public class RecordInteractionResponse {

    private String message;

    public RecordInteractionResponse() {
    }

    public RecordInteractionResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
