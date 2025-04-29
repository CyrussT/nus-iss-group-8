package com.group8.rbs.service.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class CreditRestore implements Job {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (databaseUrl == null) {
            throw new IllegalStateException("Database credentials are missing in the URL.");
        }

        String[] urlParts = databaseUrl.split("\\?");
        String url = urlParts[0]; // The main database URL without credentials
        String queryParams = urlParts.length > 1 ? urlParts[1] : "";

        String user = null;
        String password = null;

        for (String param : queryParams.split("&")) {
            String[] keyValue = param.split("=");
            if (keyValue[0].equals("user")) {
                user = keyValue[1];
            } else if (keyValue[0].equals("password")) {
                password = keyValue[1];
            }
        }

        if (user == null || password == null) {
            throw new IllegalStateException("Database credentials are missing in the URL.");
        }

        String updateQuery = "UPDATE tbl_credit SET credit_balance = 240";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                PreparedStatement stmt = conn.prepareStatement(updateQuery)) {

            int rowsUpdated = stmt.executeUpdate();
            System.out.println("Credit restore completed. Rows affected: " + rowsUpdated);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
