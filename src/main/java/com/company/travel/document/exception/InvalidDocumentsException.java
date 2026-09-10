package com.company.travel.document.exception;

import java.util.List;

public class InvalidDocumentsException extends RuntimeException {
    private final List<String> details;

    public InvalidDocumentsException(List<String> details) {
        super("Uploaded documents do not match the quotation travel location, dates, or country.");
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}