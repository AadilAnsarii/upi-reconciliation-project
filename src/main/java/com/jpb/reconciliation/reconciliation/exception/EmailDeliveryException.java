package com.jpb.reconciliation.reconciliation.exception;

/**
 * Thrown when the application fails to deliver an email
 * (SMTP error, invalid address, mail server down, etc.).
 *
 * Callers that must roll back on email failure (e.g. OTP flows)
 * should catch this specifically, clean up, and surface a
 * user-friendly message.
 */
public class EmailDeliveryException extends RuntimeException {

    private final String recipientEmail;

    public EmailDeliveryException(String message, String recipientEmail) {
        super(message);
        this.recipientEmail = recipientEmail;
    }

    public EmailDeliveryException(String message, String recipientEmail, Throwable cause) {
        super(message, cause);
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }
}
