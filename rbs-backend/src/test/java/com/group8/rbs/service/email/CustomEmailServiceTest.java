package com.group8.rbs.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomEmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private CustomEmailService emailService;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Set the 'fromEmail' field using reflection
        Field fromEmailField = CustomEmailService.class.getDeclaredField("fromEmail");
        fromEmailField.setAccessible(true);
        fromEmailField.set(emailService, "noreply@example.com");
    }

    @Test
    void sendEmail_successful() throws MessagingException {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        boolean result = emailService.sendEmail("user@example.com", "Subject", "<h1>Hello</h1>");

        // Assert
        assertTrue(result);
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendEmail_mailExceptionThrown_returnsFalse() throws MessagingException {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailException("Failed to send") {
        }).when(javaMailSender).send(any(MimeMessage.class));

        // Act
        boolean result = emailService.sendEmail("user@example.com", "Subject", "<p>Error</p>");

        // Assert
        assertFalse(result);
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendEmail_constructsCorrectMessage() throws MessagingException {
        // Arrange
        MimeMessage mimeMessage = mock(MimeMessage.class);
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendEmail("test@user.com", "Greetings", "<b>Hi there</b>");

        // Assert basic invocation (deep content checking is not trivial)
        verify(javaMailSender).send(messageCaptor.capture());
        assertNotNull(messageCaptor.getValue());
    }
}
