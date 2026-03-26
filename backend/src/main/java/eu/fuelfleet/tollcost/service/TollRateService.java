package eu.fuelfleet.tollcost.service;

import eu.fuelfleet.tollcost.entity.TollRate;
import eu.fuelfleet.tollcost.repository.TollRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TollRateService {

    private final TollRateRepository tollRateRepository;

    @Transactional(readOnly = true)
    public TollRate getCurrentRate(String countryCode, String vehicleClass) {
        return tollRateRepository.findCurrentRate(countryCode, vehicleClass)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No toll rate found for country " + countryCode + " and vehicle class " + vehicleClass));
    }

    @Transactional(readOnly = true)
    public List<TollRate> getAllRates() {
        return tollRateRepository.findAll();
    }
}
