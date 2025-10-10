package com.app.shopin.modules.user.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.user.dto.AddressDTO;
import com.app.shopin.modules.user.entity.Address;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.AddressRepository;
import com.app.shopin.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public AddressDTO addAddress(AddressDTO addressDTO, UserDetails currentUser) {
        User user = ((PrincipalUser) currentUser).getUser();

        Address address = new Address();
        address.setStreet(addressDTO.street());
        address.setInternalDetails(addressDTO.internalDetails());
        address.setCity(addressDTO.city());
        address.setState(addressDTO.state());
        address.setPostalCode(addressDTO.postalCode());
        address.setCountry(addressDTO.country());
        address.setUser(user);

        Address savedAddress = addressRepository.save(address);

        return mapToDTO(savedAddress);
    }

    @Transactional(readOnly = true)
    public List<AddressDTO> getAddresses(UserDetails currentUser) {
        User user = ((PrincipalUser) currentUser).getUser();
        List<Address> addresses = addressRepository.findByUserId(user.getId());
        return addresses.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressDTO getAddressById(Long addressId, UserDetails currentUser) {
        Address address = getAndVerifyOwnership(addressId, currentUser);
        return mapToDTO(address);
    }

    @Transactional
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO, UserDetails currentUser) {
        Address address = getAndVerifyOwnership(addressId, currentUser);

        address.setStreet(addressDTO.street());
        address.setInternalDetails(addressDTO.internalDetails());
        address.setCity(addressDTO.city());
        address.setState(addressDTO.state());
        address.setPostalCode(addressDTO.postalCode());
        address.setCountry(addressDTO.country());

        Address updatedAddress = addressRepository.save(address);
        return mapToDTO(updatedAddress);
    }

    @Transactional
    public void deleteAddress(Long addressId, UserDetails currentUser) {
        Address address = getAndVerifyOwnership(addressId, currentUser);
        addressRepository.delete(address);
    }

    private Address findAddressById(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No se encontró la dirección con ID: " + addressId));
    }

    // Métodos de ayuda

    private Address getAndVerifyOwnership(Long addressId, UserDetails currentUser) {
        User user = ((PrincipalUser) currentUser).getUser();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No se encontró la dirección con ID: " + addressId));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a esta dirección.");
        }
        return address;
    }

    private AddressDTO mapToDTO(Address address) {
        return new AddressDTO(
                address.getId(),
                address.getStreet(),
                address.getInternalDetails(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }

    // --- MÉTODO PARA ADMINS ---

    @Transactional(readOnly = true)
    public Page<AddressDTO> getAllAddresses(Pageable pageable) {
        Page<Address> addressPage = addressRepository.findAll(pageable);
        return addressPage.map(this::mapToDTO);
    }
}