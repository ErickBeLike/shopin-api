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

    private void checkOwnershipOrAdmin(Long ownerUserId, UserDetails currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));

        // 1. Hacemos un cast del UserDetails genérico a nuestra clase específica PrincipalUser
        PrincipalUser principal = (PrincipalUser) currentUser;

        // 2. "Desempaquetamos" la entidad User real
        User user = principal.getUser();

        // Ahora la comparación funciona porque estamos comparando Long con Long
        if (!ownerUserId.equals(user.getUserId()) && !isAdmin) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a este recurso.");
        }
    }

    @Transactional
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

    @Transactional(readOnly = true)
    public Page<AddressDTO> getAllAddresses(Pageable pageable) {
        Page<Address> addressPage = addressRepository.findAll(pageable);
        return addressPage.map(this::mapToDTO); // .map() es una función de Page para convertir el contenido
    }

    @Transactional(readOnly = true)
    public List<AddressDTO> getAddressesByUserId(Long userId, UserDetails currentUser) {
        // Verificamos que el usuario que pide la lista sea el dueño O un admin
        checkOwnershipOrAdmin(userId, currentUser);

        if (!userRepository.existsById(userId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "No se encontró el usuario con ID: " + userId);
        }
        List<Address> addresses = addressRepository.findByUserUserId(userId);
        return addresses.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressDTO getAddressById(Long addressId, UserDetails currentUser) {
        Address address = findAddressById(addressId);

        // Verificamos que el usuario que pide la dirección sea el dueño O un admin
        checkOwnershipOrAdmin(address.getUser().getUserId(), currentUser);

        return mapToDTO(address);
    }

    @Transactional
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

    @Transactional
    public void deleteAddress(Long addressId) {
        Address address = findAddressById(addressId);
        addressRepository.delete(address);
    }

    private Address findAddressById(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No se encontró la dirección con ID: " + addressId));
    }

    private AddressDTO mapToDTO(Address address) {
        return new AddressDTO(
                address.getAddressId(),
                address.getStreet(),
                address.getInternalDetails(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }
}