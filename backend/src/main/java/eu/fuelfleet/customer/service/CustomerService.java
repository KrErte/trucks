package eu.fuelfleet.customer.service;

import eu.fuelfleet.auth.entity.User;
import eu.fuelfleet.auth.repository.UserRepository;
import eu.fuelfleet.customer.dto.CustomerRequest;
import eu.fuelfleet.customer.dto.CustomerResponse;
import eu.fuelfleet.customer.entity.Customer;
import eu.fuelfleet.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public List<CustomerResponse> getAll() {
        return customerRepository.findByCompanyId(getCompanyId()).stream()
                .map(CustomerResponse::from).toList();
    }

    public List<CustomerResponse> getActive() {
        return customerRepository.findByCompanyIdAndActiveTrue(getCompanyId()).stream()
                .map(CustomerResponse::from).toList();
    }

    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        User user = getCurrentUser();
        Customer c = Customer.builder()
                .company(user.getCompany()).name(req.getName()).vatNumber(req.getVatNumber())
                .regCode(req.getRegCode()).email(req.getEmail()).phone(req.getPhone())
                .address(req.getAddress()).country(req.getCountry())
                .contactPerson(req.getContactPerson())
                .paymentTermDays(req.getPaymentTermDays()).notes(req.getNotes()).active(true)
                .build();
        return CustomerResponse.from(customerRepository.save(c));
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest req) {
        Customer c = customerRepository.findById(id)
                .filter(cu -> cu.getCompany().getId().equals(getCompanyId()))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        c.setName(req.getName()); c.setVatNumber(req.getVatNumber());
        c.setRegCode(req.getRegCode()); c.setEmail(req.getEmail());
        c.setPhone(req.getPhone()); c.setAddress(req.getAddress());
        c.setCountry(req.getCountry()); c.setContactPerson(req.getContactPerson());
        c.setPaymentTermDays(req.getPaymentTermDays()); c.setNotes(req.getNotes());
        return CustomerResponse.from(customerRepository.save(c));
    }

    @Transactional
    public void delete(UUID id) {
        customerRepository.findById(id)
                .filter(c -> c.getCompany().getId().equals(getCompanyId()))
                .ifPresent(customerRepository::delete);
    }

    private UUID getCompanyId() { return getCurrentUser().getCompany().getId(); }
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }
}
