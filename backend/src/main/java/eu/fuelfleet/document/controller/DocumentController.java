package eu.fuelfleet.document.controller;

import eu.fuelfleet.document.dto.DocumentRequest;
import eu.fuelfleet.document.dto.DocumentResponse;
import eu.fuelfleet.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @GetMapping
    public List<DocumentResponse> getAll() { return documentService.getAll(); }

    @PostMapping
    public DocumentResponse create(@RequestBody DocumentRequest request) { return documentService.create(request); }

    @PutMapping("/{id}")
    public DocumentResponse update(@PathVariable UUID id, @RequestBody DocumentRequest request) { return documentService.update(id, request); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { documentService.delete(id); return ResponseEntity.noContent().build(); }
}
