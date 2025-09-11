package com.app.shopin.modules.user.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.user.dto.AddressDTO;
import com.app.shopin.modules.user.entity.Address;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.AddressRepository;
import com.app.shopin.modules.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Añade una nueva dirección a un usuario específico.
     */
    public AddressDTO addAddressToUser(Long userId, AddressDTO addressDTO) {
        // 1. Buscamos al usuario para asociarle la dirección
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No se encontró el usuario con ID: " + userId));

        // 2. Creamos la nueva entidad Address a partir del DTO
        Address address = new Address();
        address.setStreet(addressDTO.street());
        address.setInternalDetails(addressDTO.internalDetails());
        address.setCity(addressDTO.city());
        address.setState(addressDTO.state());
        address.setPostalCode(addressDTO.postalCode());
        address.setCountry(addressDTO.country());

        // 3. Establecemos la relación
        address.setUser(user);

        // 4. Guardamos la nueva dirección
        Address savedAddress = addressRepository.save(address);

        // 5. Devolvemos el DTO de la dirección guardada
        return mapToDTO(savedAddress);
    }

    /**
     * Obtiene todas las direcciones de un usuario.
     */
    public List<AddressDTO> getAddressesByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "No se encontró el usuario con ID: " + userId);
        }
        List<Address> addresses = addressRepository.findByUserUserId(userId);
        return addresses.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza una dirección existente.
     */
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address address = findAddressById(addressId);

        address.setStreet(addressDTO.street());
        address.setInternalDetails(addressDTO.internalDetails());
        address.setCity(addressDTO.city());
        address.setState(addressDTO.state());
        address.setPostalCode(addressDTO.postalCode());
        address.setCountry(addressDTO.country());

        Address updatedAddress = addressRepository.save(address);
        return mapToDTO(updatedAddress);
    }

    /**
     * Elimina una dirección por su ID.
     */
    public void deleteAddress(Long addressId) {
        Address address = findAddressById(addressId);
        addressRepository.delete(address);
    }

    // --- Métodos privados de ayuda ---

    private Address findAddressById(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No se encontró la dirección con ID: " + addressId));
    }

    private AddressDTO mapToDTO(Address address) {
        return new AddressDTO(
                address.getStreet(),
                address.getInternalDetails(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }
}