package com.gateway;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
    RequestMethod.GET, RequestMethod.POST, 
    RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@RequestMapping("/gateway")
public class GatewayController {

    private RestTemplate restTemplate = new RestTemplate();

    private static final String JSON_SERVICE = System.getenv("JSON_SERVICE_URL") != null
            ? System.getenv("JSON_SERVICE_URL")
            : "http://localhost:8084";

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(
                JSON_SERVICE + "/users/" + id,
                HttpMethod.GET, entity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader("Authorization") String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(
                JSON_SERVICE + "/users",
                HttpMethod.GET, entity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(
            @RequestHeader("Authorization") String token,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String phone) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            String url = JSON_SERVICE + "/users?name=" + name + "&email=" + email;
            if (bio != null) url += "&bio=" + bio;
            if (phone != null) url += "&phone=" + phone;
            return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(
            @RequestHeader("Authorization") String token,
            @PathVariable String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String phone) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            String url = JSON_SERVICE + "/users/" + id + "?";
            if (name != null) url += "name=" + name + "&";
            if (bio != null) url += "bio=" + bio + "&";
            if (phone != null) url += "phone=" + phone;
            return restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(
            @RequestHeader("Authorization") String token,
            @PathVariable String id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(
                JSON_SERVICE + "/users/" + id,
                HttpMethod.DELETE, entity, String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @GetMapping("/auth/register")
    public ResponseEntity<?> register(
            @RequestParam String username,
            @RequestParam String password) {
        try {
            return restTemplate.getForEntity(
                JSON_SERVICE + "/auth/register?username=" + username + "&password=" + password,
                String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestParam String username,
            @RequestParam String password) {
        try {
            return restTemplate.getForEntity(
                JSON_SERVICE + "/auth/login?username=" + username + "&password=" + password,
                String.class);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}