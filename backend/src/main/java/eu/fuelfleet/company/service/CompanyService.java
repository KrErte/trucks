package eu.fuelfleet.company.service;

import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Company getCompany(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    @Transactional
    public Company createCompany(Company company) {
        return companyRepository.save(company);
    }

    @Transactional
    public Company updateCompany(UUID id, Company updates) {
        Company company = getCompany(id);
        company.setName(updates.getName());
        company.setVatNumber(updates.getVatNumber());
        company.setCountry(updates.getCountry());
        company.setDefaultDriverDailyRate(updates.getDefaultDriverDailyRate());
        return companyRepository.save(company);
    }

    @Transactional
    public void deleteCompany(UUID id) {
        if (!companyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found");
        }
        companyRepository.deleteById(id);
    }
}
