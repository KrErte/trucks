package eu.fuelfleet.fuel.service;

import eu.fuelfleet.fuel.entity.FuelPrice;
import eu.fuelfleet.fuel.repository.FuelPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FuelPriceService {

    private final FuelPriceRepository fuelPriceRepository;

    @Transactional(readOnly = true)
    public FuelPrice getCurrentPrice(String countryCode, String fuelType) {
        return fuelPriceRepository.findCurrentPrice(countryCode, fuelType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No fuel price found for country " + countryCode + " and fuel type " + fuelType));
    }

    @Transactional(readOnly = true)
    public List<FuelPrice> getPricesByCountry(String countryCode) {
        return fuelPriceRepository.findByCountryCodeOrderByValidFromDesc(countryCode);
    }

    @Transactional
    public FuelPrice updatePrice(FuelPrice price) {
        return fuelPriceRepository.save(price);
    }
}
