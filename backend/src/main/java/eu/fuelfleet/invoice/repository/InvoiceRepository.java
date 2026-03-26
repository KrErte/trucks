package eu.fuelfleet.invoice.repository;

import eu.fuelfleet.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    @Query(value = "SELECT COUNT(*) + 1 FROM invoices WHERE company_id = :companyId AND EXTRACT(YEAR FROM created_at) = EXTRACT(YEAR FROM CURRENT_DATE)", nativeQuery = true)
    Long getNextInvoiceSequence(@Param("companyId") UUID companyId);
}
