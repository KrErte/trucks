package eu.fuelfleet.customer.repository;

import eu.fuelfleet.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    List<Customer> findByCompanyIdAndActiveTrue(UUID companyId);
    List<Customer> findByCompanyId(UUID companyId);
}
