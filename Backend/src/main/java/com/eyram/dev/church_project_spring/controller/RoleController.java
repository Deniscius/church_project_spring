package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.RoleRequest;
import com.eyram.dev.church_project_spring.DTO.response.RoleResponse;
import com.eyram.dev.church_project_spring.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleResponse> create(@RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.create(request));
    }


    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAll() {
        return ResponseEntity.ok(roleService.getAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getById(id));
    }


    @GetMapping("/public/{publicId}")
    public ResponseEntity<RoleResponse> getByPublicId(@PathVariable String publicId) {
        return ResponseEntity.ok(roleService.getByPublicId(publicId));
    }


    @PutMapping("/public/{publicId}")
    public ResponseEntity<RoleResponse> update(@PathVariable String publicId, @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.update(publicId, request));
    }

    @DeleteMapping("/public/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable String publicId) {
        roleService.delete(publicId);
        return ResponseEntity.noContent().build();
    }
}
