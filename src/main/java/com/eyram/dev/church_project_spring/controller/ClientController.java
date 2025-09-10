package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ClientRequest;
import com.eyram.dev.church_project_spring.DTO.response.ClientResponse;
import com.eyram.dev.church_project_spring.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest client) {
        ClientResponse response = clientService.create(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/search/by-name/{nom}")
    public ResponseEntity<List<ClientResponse>> getByName(@PathVariable String nom) {
        List<ClientResponse> clients = clientService.getNom(nom);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/search/by-id/{publicId}")
    public ResponseEntity<ClientResponse> getByPublicId(@PathVariable UUID publicId) {
        ClientResponse client = clientService.getByPublicId(publicId);
        return ResponseEntity.ok(client);
    }

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAll() {
        return ResponseEntity.ok(clientService.getAll());
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<ClientResponse> update(@PathVariable UUID publicId,
                                                 @Valid @RequestBody ClientRequest client) {
        ClientResponse response = clientService.update(publicId, client);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID publicId) {
        clientService.softDelete(publicId);
        return ResponseEntity.ok(Map.of("message", "Client supprimé"));
    }

    @GetMapping("/active")
    public ResponseEntity<?> getInactive() {
        List<ClientResponse> inactifs = clientService.getInactive();
        if (inactifs.isEmpty()) {
            return ResponseEntity.ok("Aucun client inactif trouvé");
        }
        return ResponseEntity.ok(inactifs);
    }

    //supp=false



}
