package com.app.shopin.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public class AddressDTO {
    @NotBlank
    private String street;
    private String internalDetails;
    @NotBlank
    private String city;
    @NotBlank
    private String state;
    @NotBlank
    private String postalCode;
    @NotBlank
    private String country;

    public AddressDTO(String street, String internalDetails, String city, String state, String postalCode, String country) {
        this.street = street;
        this.internalDetails = internalDetails;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public @NotBlank String getStreet() {
        return street;
    }

    public void setStreet(@NotBlank String street) {
        this.street = street;
    }

    public @NotBlank String getInternalDetails() {
        return internalDetails;
    }

    public void setInternalDetails(@NotBlank String internalDetails) {
        this.internalDetails = internalDetails;
    }

    public @NotBlank String getCity() {
        return city;
    }

    public void setCity(@NotBlank String city) {
        this.city = city;
    }

    public @NotBlank String getState() {
        return state;
    }

    public void setState(@NotBlank String state) {
        this.state = state;
    }

    public @NotBlank String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(@NotBlank String postalCode) {
        this.postalCode = postalCode;
    }

    public @NotBlank String getCountry() {
        return country;
    }

    public void setCountry(@NotBlank String country) {
        this.country = country;
    }
}
