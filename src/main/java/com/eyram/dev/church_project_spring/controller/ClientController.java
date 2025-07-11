package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.models.Client;
import com.eyram.dev.church_project_spring.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Client> create(@RequestBody Client client) {
        return new ResponseEntity<>(clientService.create(client), HttpStatus.CREATED);
    }

    @GetMapping("/{nom}")
    public ResponseEntity<Client> getbyname(@PathVariable String nom) {
        return new ResponseEntity<>(clientService.getNom(nom), HttpStatus.OK);
    }

    @PutMapping("/{nom}")
    public ResponseEntity<Client> update(@PathVariable String nom, @RequestBody Client client) {
        return new ResponseEntity<>(clientService.update(nom, client), HttpStatus.OK);

    }

    @DeleteMapping ("/{nom}")
    public ResponseEntity<String> delete(@PathVariable String nom) {
        clientService.delete(nom);
        return new ResponseEntity<>  ("Client supprimé", HttpStatus.OK);
    }

}
