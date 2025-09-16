package com.app.shopin.modules.user.entity;

// import com.app.shopin.entity.employee.Employee;
import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.entity.SocialLink;
import com.app.shopin.modules.security.enums.TwoFactorMethod;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_id = ?")
@Where(clause = "deleted_at IS NULL")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String profilePictureUrl;
    @Column(columnDefinition = "TEXT")
    private String profilePicturePublicId;

    // THESE ATTRIBUTES ARE IMPORTANT FOR THE SECURITY MODULE
    @Column(nullable = false, unique = true)
    private String userName;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SocialLink> socialLinks = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    @Column(nullable = false)
    private Integer tokenVersion = 0;

    @Column(nullable = true)
    private String passwordResetCode;
    @Column(nullable = true)
    private LocalDateTime resetCodeExpiration;

    /*
    @Column(nullable = false)
    private boolean twoFactorEnabled = false;
    @Column
    private String twoFactorSecret;
     */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TwoFactorMethod preferredTwoFactorMethod = TwoFactorMethod.NONE;

    // 2FA APP
    @Column(nullable = false)
    private boolean twoFactorAppEnabled = false;
    @Column
    private String twoFactorSecret;

    // 2FA EMAIL
    @Column(nullable = false)
    private boolean twoFactorEmailEnabled = false;
    @Column
    private String twoFactorEmailCode;
    @Column
    private LocalDateTime twoFactorCodeExpiration;
    //---

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Address> addresses = new HashSet<>();

    // @OneToOne(mappedBy = "user")  // Relación bidireccional
    // private Employee employee;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    @Column()
    private LocalDateTime updatedAt;
    @Column
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public User() {
    }

    public User(@NotBlank String userName, @NotBlank String email, @NotBlank String password) {
        this.userName = userName;
        this.email = email;
        this.password = password;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public Set<SocialLink> getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(Set<SocialLink> socialLinks) {
        this.socialLinks = socialLinks;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }
    public void setTokenVersion(Integer tokenVersion) {
        this.tokenVersion = tokenVersion;
    }
    /** Llamar justo antes de guardar cuando cambies email/password/username */
    public void incrementTokenVersion() {
        this.tokenVersion++;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Set<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<Address> addresses) {
        this.addresses = addresses;
    }

    public Set<Rol> getRoles() {
        return roles;
    }

    public void setRoles(Set<Rol> roles) {
        this.roles = roles;
    }

    public String getPasswordResetCode() {
        return passwordResetCode;
    }

    public void setPasswordResetCode(String passwordResetCode) {
        this.passwordResetCode = passwordResetCode;
    }

    public LocalDateTime getResetCodeExpiration() {
        return resetCodeExpiration;
    }

    public void setResetCodeExpiration(LocalDateTime resetCodeExpiration) {
        this.resetCodeExpiration = resetCodeExpiration;
    }

    public TwoFactorMethod getPreferredTwoFactorMethod() {
        return preferredTwoFactorMethod;
    }

    public void setPreferredTwoFactorMethod(TwoFactorMethod preferredTwoFactorMethod) {
        this.preferredTwoFactorMethod = preferredTwoFactorMethod;
    }

    public boolean isTwoFactorAppEnabled() {
        return twoFactorAppEnabled;
    }

    public void setTwoFactorAppEnabled(boolean twoFactorAppEnabled) {
        this.twoFactorAppEnabled = twoFactorAppEnabled;
    }

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
    }

    public boolean isTwoFactorEmailEnabled() {
        return twoFactorEmailEnabled;
    }

    public void setTwoFactorEmailEnabled(boolean twoFactorEmailEnabled) {
        this.twoFactorEmailEnabled = twoFactorEmailEnabled;
    }

    public String getTwoFactorEmailCode() {
        return twoFactorEmailCode;
    }

    public void setTwoFactorEmailCode(String twoFactorEmailCode) {
        this.twoFactorEmailCode = twoFactorEmailCode;
    }

    public LocalDateTime getTwoFactorCodeExpiration() {
        return twoFactorCodeExpiration;
    }

    public void setTwoFactorCodeExpiration(LocalDateTime twoFactorCodeExpiration) {
        this.twoFactorCodeExpiration = twoFactorCodeExpiration;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getProfilePicturePublicId() {
        return profilePicturePublicId;
    }

    public void setProfilePicturePublicId(String profilePicturePublicId) {
        this.profilePicturePublicId = profilePicturePublicId;
    }

    /** public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    } **/

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
