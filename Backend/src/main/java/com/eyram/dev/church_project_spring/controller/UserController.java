package com.eyram.dev.church_project_spring.controller;

import com.eyram.dev.church_project_spring.DTO.request.UserRequest;
import com.eyram.dev.church_project_spring.DTO.response.UserResponse;
import com.eyram.dev.church_project_spring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return new ResponseEntity<>(userService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{publicId}")
    public ResponseEntity<UserResponse> update(@PathVariable UUID publicId,
                                               @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.update(publicId, request));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<UserResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(userService.getByPublicId(publicId));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        userService.delete(publicId);
        return ResponseEntity.noContent().build();
    }
}