package com.app.shopin.modules.payment.entity;

import com.app.shopin.modules.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String paymentProviderToken;

    @Column(nullable = false)
    private String cardBrand; // "Visa", "Mastercard"

    @Column(nullable = false)
    private String lastFourDigits; // "XXXX"

    @Column(nullable = false)
    private Integer expirationMonth; // 1, 2, ..., 12

    @Column(nullable = false)
    private Integer expirationYear; // 2028, 2030

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPaymentProviderToken() {
        return paymentProviderToken;
    }

    public void setPaymentProviderToken(String paymentProviderToken) {
        this.paymentProviderToken = paymentProviderToken;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    public String getLastFourDigits() {
        return lastFourDigits;
    }

    public void setLastFourDigits(String lastFourDigits) {
        this.lastFourDigits = lastFourDigits;
    }

    public Integer getExpirationMonth() {
        return expirationMonth;
    }

    public void setExpirationMonth(Integer expirationMonth) {
        this.expirationMonth = expirationMonth;
    }

    public Integer getExpirationYear() {
        return expirationYear;
    }

    public void setExpirationYear(Integer expirationYear) {
        this.expirationYear = expirationYear;
    }
}