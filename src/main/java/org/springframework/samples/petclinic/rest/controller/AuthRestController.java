package org.springframework.samples.petclinic.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.model.Role;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.rest.dto.LoginRequest;
import org.springframework.samples.petclinic.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(exposedHeaders = "errors, content-type")
@RequestMapping("api/auth")
public class AuthRestController {

    private final UserService userService;

    public AuthRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isEmpty() ||
            loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Username and password are required");
            return ResponseEntity.badRequest().body(error);
        }

        boolean valid = userService.validateCredentials(
            loginRequest.getUsername(),
            loginRequest.getPassword()
        );

        if (valid) {
            User user = userService.findByUsername(loginRequest.getUsername());
            List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("username", user.getUsername());
            response.put("roles", roles);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}

