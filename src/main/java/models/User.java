package models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDateTime accountCreated;

    @Column(nullable = false)
    private LocalDateTime accountUpdated;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean emailVerified = false;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
        
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDateTime getAccountCreated() {
        return accountCreated;
    }
    
    public void setAccountCreated(LocalDateTime accountCreated) {
        this.accountCreated = accountCreated;
    }
    
    public LocalDateTime getAccountUpdated() {
        return accountUpdated;
    }
    
    public void setAccountUpdated(LocalDateTime accountUpdated) {
        this.accountUpdated = accountUpdated;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
