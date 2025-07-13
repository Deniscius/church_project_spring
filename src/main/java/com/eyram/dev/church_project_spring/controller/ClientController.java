package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.ClientRequest;
import com.eyram.dev.church_project_spring.DTO.response.ClientResponse;
import com.eyram.dev.church_project_spring.models.Client;
import com.eyram.dev.church_project_spring.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/Clients")
@RestController
@CrossOrigin("*")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@RequestBody ClientRequest client) {
        return new ResponseEntity<>(clientService.create(client), HttpStatus.CREATED);
    }

    @GetMapping("/{nom}")
    public ResponseEntity<ClientResponse> getbyname(@PathVariable String nom) {
        return new ResponseEntity<>(clientService.getNom(nom), HttpStatus.OK);
    }

    @GetMapping("/searchby/{publicId}")
    public ResponseEntity<ClientResponse> getbypublicId(@PathVariable UUID publicId) {
        return new ResponseEntity<>(clientService.getByPublicId(publicId), HttpStatus.OK);
    }

    @GetMapping()
    public ResponseEntity<List<ClientResponse>> getAll() {
        return new ResponseEntity<>(clientService.getAll(), HttpStatus.OK);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<ClientResponse> update(@PathVariable UUID publicId, @RequestBody ClientRequest client) {
        return new ResponseEntity<>(clientService.update(publicId, client), HttpStatus.OK);

    }

    @DeleteMapping ("/{publicId}")
    public ResponseEntity<String> delete(@PathVariable UUID publicId) {
        clientService.delete(publicId);
        return new ResponseEntity<>  ("Client supprimé", HttpStatus.OK);
    }

}
