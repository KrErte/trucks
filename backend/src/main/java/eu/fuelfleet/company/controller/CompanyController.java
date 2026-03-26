package eu.fuelfleet.company.controller;

import eu.fuelfleet.auth.security.UserPrincipal;
import eu.fuelfleet.company.entity.Company;
import eu.fuelfleet.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public CompanyResponse getCompany(@AuthenticationPrincipal UserPrincipal principal) {
        Company company = companyService.getCompany(principal.getCompanyId());
        return CompanyResponse.fromEntity(company);
    }

    @PutMapping
    public CompanyResponse updateCompany(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CompanyUpdateRequest request) {
        Company updates = Company.builder()
                .name(request.companyName())
                .vatNumber(request.vatNumber())
                .country(request.country())
                .defaultDriverDailyRate(request.defaultDriverDailyRate())
                .build();
        Company updated = companyService.updateCompany(principal.getCompanyId(), updates);
        return CompanyResponse.fromEntity(updated);
    }

    public record CompanyResponse(
            String companyName,
            String vatNumber,
            String country,
            BigDecimal defaultDriverDailyRate
    ) {
        public static CompanyResponse fromEntity(Company c) {
            return new CompanyResponse(c.getName(), c.getVatNumber(), c.getCountry(), c.getDefaultDriverDailyRate());
        }
    }

    public record CompanyUpdateRequest(
            String companyName,
            String vatNumber,
            String country,
            BigDecimal defaultDriverDailyRate
    ) {}
}
