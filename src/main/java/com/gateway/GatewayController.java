package com.gateway;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/gateway")
public class GatewayController {

    private RestTemplate restTemplate = new RestTemplate();
    private static final String JSON_SERVICE = System.getenv("JSON_SERVICE_URL") != null 
    	    ? System.getenv("JSON_SERVICE_URL") 
    	    : "http://localhost:8084";

    // Forward all requests to JSON service
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(
            JSON_SERVICE + "/users/" + id,
            HttpMethod.GET, entity, String.class);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(
            @RequestHeader("Authorization") String token,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String phone) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(
            JSON_SERVICE + "/users?name=" + name + "&email=" + email + "&bio=" + bio + "&phone=" + phone,
            HttpMethod.POST, entity, String.class);
    }

    @GetMapping("/auth/register")
    public ResponseEntity<?> register(
            @RequestParam String username,
            @RequestParam String password) {
        return restTemplate.getForEntity(
            JSON_SERVICE + "/auth/register?username=" + username + "&password=" + password,
            String.class);
    }

    @GetMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestParam String username,
            @RequestParam String password) {
        return restTemplate.getForEntity(
            JSON_SERVICE + "/auth/login?username=" + username + "&password=" + password,
            String.class);
    }
}