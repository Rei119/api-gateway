package com.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
    RequestMethod.GET, RequestMethod.POST,
    RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS
})
@RequestMapping("/gateway")
public class GatewayController {

    private RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${json.service.url}")
    private String JSON_SERVICE;

    @Value("${file.service.url}")
    private String FILE_SERVICE;

    private static final int CACHE_TTL = 60;

    private ResponseEntity<?> getCached(String cacheKey, String url, HttpHeaders headers) {
        try {
            String cached = null;
            try {
                cached = redisTemplate.opsForValue().get(cacheKey);
            } catch (Exception redisEx) {
                System.out.println("Redis unavailable, skipping cache: " + redisEx.getMessage());
            }

            if (cached != null) {
                System.out.println("CACHE HIT: " + cacheKey);
                return ResponseEntity.ok()
                    .header("X-Cache", "HIT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cached);
            }

            System.out.println("CACHE MISS: " + cacheKey);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, response.getBody(), CACHE_TTL, TimeUnit.SECONDS);
                } catch (Exception redisEx) {
                    System.out.println("Redis unavailable, skipping cache store: " + redisEx.getMessage());
                }
            }

            return ResponseEntity.ok()
                .header("X-Cache", "MISS")
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());

        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void invalidateCache(String... keys) {
        try {
            for (String key : keys) {
                redisTemplate.delete(key);
                System.out.println("CACHE INVALIDATED: " + key);
            }
        } catch (Exception redisEx) {
            System.out.println("Redis unavailable, skipping cache invalidation: " + redisEx.getMessage());
        }
    }

    @GetMapping("/auth/register")
    public ResponseEntity<?> register(@RequestParam String username, @RequestParam String password) {
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
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
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

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        return getCached("users:all", JSON_SERVICE + "/users", headers);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id,
                                      @RequestHeader("Authorization") String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        return getCached("users:" + id, JSON_SERVICE + "/users/" + id, headers);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestHeader("Authorization") String token,
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
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            invalidateCache("users:all");
            return response;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@RequestHeader("Authorization") String token,
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
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            invalidateCache("users:" + id, "users:all");
            return response;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String token,
                                         @PathVariable String id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                JSON_SERVICE + "/users/" + id, HttpMethod.DELETE, entity, String.class);
            invalidateCache("users:" + id, "users:all");
            return response;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PatchMapping("/users/{id}/image")
    public ResponseEntity<?> updateImage(@RequestHeader("Authorization") String token,
                                          @PathVariable String id,
                                          @RequestParam String imageUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = JSON_SERVICE + "/users/" + id + "/image?imageUrl=" + imageUrl;

            ResponseEntity<String> response = restTemplate.exchange(
                new java.net.URI(url), HttpMethod.PATCH, entity, String.class);
            invalidateCache("users:" + id, "users:all");
            return response;
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}