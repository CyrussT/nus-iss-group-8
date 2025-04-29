package com.group8.rbs.service.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.group8.rbs.entities.Account;
import com.group8.rbs.enums.AccountType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private Account testAccount;
    private UserDetails userDetails;
    private String testSecretKey = "2B4B6250655368566D597133743677397A244226452948404D635166546A576E";
    private Key signingKey;

    @BeforeEach
    void setUp() {
        // Set up test account
        testAccount = Account.builder()
                .email("test@example.com")
                .accountType(AccountType.STUDENT)
                .build();
        
        // Set up UserDetails
        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
        
        // Set the secret key using reflection
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);
        
        // Create signing key for test usage
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecretKey));
    }

    @Test
    void generateTokenShouldCreateValidToken() {
        // Generate a token
        String token = jwtService.generateToken(testAccount);
        
        // Assert that token is not null or empty
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Verify claims in the token
        Claims claims = Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        assertEquals("test@example.com", claims.getSubject());
        assertEquals(AccountType.STUDENT.name(), claims.get("role"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }
    
    @Test
    void extractEmailShouldReturnEmailFromToken() {
        // Generate a token
        String token = jwtService.generateToken(testAccount);
        
        // Extract the email
        String email = jwtService.extractEmail(token);
        
        // Verify the extracted email
        assertEquals("test@example.com", email);
    }
    
    @Test
    void isTokenValidShouldReturnTrueForValidToken() {
        // Generate a token
        String token = jwtService.generateToken(testAccount);
        
        // Test validity for correct user
        boolean isValid = jwtService.isTokenValid(token, userDetails);
        
        // Verify the token is valid
        assertTrue(isValid);
    }
    
    @Test
    void isTokenValidShouldReturnFalseForInvalidUsername() {
        // Generate a token
        String token = jwtService.generateToken(testAccount);
        
        // Create UserDetails with different username
        UserDetails differentUser = User.builder()
                .username("different@example.com")
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
        
        // Test validity for incorrect user
        boolean isValid = jwtService.isTokenValid(token, differentUser);
        
        // Verify the token is invalid
        assertFalse(isValid);
    }
    
    @Test
    void isTokenValidShouldHandleExpiredToken() {
        // We need to create a correctly signed but expired token
        String expiredToken = generateExpiredToken(testAccount);
        
        try {
            // This should throw an ExpiredJwtException when extracting the email
            jwtService.extractEmail(expiredToken);
            fail("Expected ExpiredJwtException was not thrown");
        } catch (ExpiredJwtException e) {
            // Expected exception
        }
        
        // The isTokenValid method should handle the exception and return false
        boolean isValid = jwtService.isTokenValid(expiredToken, userDetails);
        
        // The token should be considered invalid
        assertFalse(isValid);
    }
    
    @Test
    void extractClaimShouldReturnClaimFromToken() {
        // Generate a token
        String token = jwtService.generateToken(testAccount);
        
        // Extract a claim
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        
        // Verify the extracted claim
        assertEquals(AccountType.STUDENT.name(), role);
    }
    
    /**
     * Helper method to generate an expired token for testing
     */
    private String generateExpiredToken(Account account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", account.getAccountType().name());
        
        return Jwts.builder()
                .claims(claims)
                .subject(account.getEmail())
                .issuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 48)) // 2 days ago
                .expiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24)) // 1 day ago (expired)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}