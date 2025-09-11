package com.app.shopin.modules.user.controller;

import com.app.shopin.modules.user.dto.AddressDTO;
import com.app.shopin.modules.user.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping("/addresses")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Page<AddressDTO>> getAllAddresses(Pageable pageable) {
        Page<AddressDTO> addresses = addressService.getAllAddresses(pageable);
        return ResponseEntity.ok(addresses);
    }

    @PostMapping("/users/{userId}/addresses")
    public ResponseEntity<AddressDTO> addAddressToUser(
            @PathVariable Long userId,
            @Valid @RequestBody AddressDTO addressDTO) {
        AddressDTO newAddress = addressService.addAddressToUser(userId, addressDTO);
        return new ResponseEntity<>(newAddress, HttpStatus.CREATED);
    }

    @GetMapping("/users/{userId}/addresses")
    public ResponseEntity<List<AddressDTO>> getAddressesByUserId(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails currentUser) {

        List<AddressDTO> addresses = addressService.getAddressesByUserId(userId, currentUser);
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDTO> getAddressById(
            @PathVariable Long addressId,
            @AuthenticationPrincipal UserDetails currentUser) {

        AddressDTO address = addressService.getAddressById(addressId, currentUser);
        return ResponseEntity.ok(address);
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressDTO addressDTO) {
        AddressDTO updatedAddress = addressService.updateAddress(addressId, addressDTO);
        return ResponseEntity.ok(updatedAddress);
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
        addressService.deleteAddress(addressId);
        return ResponseEntity.noContent().build();
    }
}