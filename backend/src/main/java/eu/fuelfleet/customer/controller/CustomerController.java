package eu.fuelfleet.customer.controller;

import eu.fuelfleet.customer.dto.CustomerRequest;
import eu.fuelfleet.customer.dto.CustomerResponse;
import eu.fuelfleet.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping
    public List<CustomerResponse> getAll() { return customerService.getAll(); }

    @GetMapping("/active")
    public List<CustomerResponse> getActive() { return customerService.getActive(); }

    @PostMapping
    public CustomerResponse create(@RequestBody CustomerRequest request) { return customerService.create(request); }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id, @RequestBody CustomerRequest request) { return customerService.update(id, request); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { customerService.delete(id); return ResponseEntity.noContent().build(); }
}
