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
@RequestMapping("/api/addresses")
@CrossOrigin
public class AddressController {

    @Autowired
    private AddressService addressService;

    @PostMapping("/me")
    public ResponseEntity<AddressDTO> addAddress(
            @Valid @RequestBody AddressDTO addressDTO,
            @AuthenticationPrincipal UserDetails currentUser) {
        AddressDTO newAddress = addressService.addAddress(addressDTO, currentUser);
        return new ResponseEntity<>(newAddress, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<List<AddressDTO>> getAddresses(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(addressService.getAddresses(currentUser));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressDTO> getAddressById(
            @PathVariable Long addressId,
            @AuthenticationPrincipal UserDetails currentUser) {

        AddressDTO address = addressService.getAddressById(addressId, currentUser);
        return ResponseEntity.ok(address);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDTO> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressDTO addressDTO,
            @AuthenticationPrincipal UserDetails currentUser) {
        AddressDTO updatedAddress = addressService.updateAddress(addressId, addressDTO, currentUser);
        return ResponseEntity.ok(updatedAddress);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long addressId,
            @AuthenticationPrincipal UserDetails currentUser) {
        addressService.deleteAddress(addressId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<Page<AddressDTO>> getAllAddresses(Pageable pageable) {
        return ResponseEntity.ok(addressService.getAllAddresses(pageable));
    }
}