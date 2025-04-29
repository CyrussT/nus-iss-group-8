package com.group8.rbs.service.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditRestoreTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockStatement;

    @InjectMocks
    private CreditRestore creditRestore;

    @Test
    void testExecute_invalidUrl_credentialsMissing() throws Exception {
        CreditRestore creditRestoreWithInvalidUrl = new CreditRestore();
        Field databaseUrlField = CreditRestore.class.getDeclaredField("databaseUrl");
        databaseUrlField.setAccessible(true);

        databaseUrlField.set(creditRestoreWithInvalidUrl, "jdbc:h2:mem:testdb");

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            creditRestoreWithInvalidUrl.execute(null);
        });

        assertEquals("Database credentials are missing in the URL.", exception.getMessage());
    }

    @Test
    void testExecute_nullUrl() throws Exception {
        CreditRestore creditRestoreWithNullUrl = new CreditRestore();
        Field databaseUrlField = CreditRestore.class.getDeclaredField("databaseUrl");
        databaseUrlField.setAccessible(true);
        databaseUrlField.set(creditRestoreWithNullUrl, null);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            creditRestoreWithNullUrl.execute(null);
        });

        assertEquals("Database credentials are missing in the URL.", exception.getMessage());
    }
}
