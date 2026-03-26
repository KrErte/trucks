package eu.fuelfleet.company.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/company-search")
@RequiredArgsConstructor
public class CompanySearchController {

    private final RestTemplate restTemplate;

    @GetMapping
    public ResponseEntity<String> search(@RequestParam String q) {
        if (q == null || q.length() < 2) {
            return ResponseEntity.ok("[]");
        }
        String url = "https://ariregister.rik.ee/est/api/autocomplete?q=" + q;
        String result = restTemplate.getForObject(url, String.class);
        return ResponseEntity.ok(result);
    }
}
