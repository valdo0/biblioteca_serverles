package com.sumativa1.biblioteca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RestTemplate restTemplate;

    @Value("${azure.eventgrid.topic.endpoint}")
    private String functionUrl;

    @Value("${azure.eventgrid.topic.key}")
    private String topicKey;

    public RoleController() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    private HttpEntity<String> jsonEntity(String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("aeg-sas-key", topicKey);
        headers.set("aeg-event-type", "Notification");

        String eventGridPayload = "[\n" +
                "  {\n" +
                "    \"id\": \"" + java.util.UUID.randomUUID().toString() + "\",\n" +
                "    \"eventType\": \"RoleAssigned\",\n" +
                "    \"subject\": \"ms-roles/assign\",\n" +
                "    \"eventTime\": \"" + java.time.Instant.now().toString() + "\",\n" +
                "    \"data\": " + payload + ",\n" +
                "    \"dataVersion\": \"1.0\"\n" +
                "  }\n" +
                "]";

        return new HttpEntity<>(eventGridPayload, headers);
    }

    @PostMapping("/assign")
    public ResponseEntity<String> assignRole(@RequestBody String payload) {
        // Here we would normally save to the Oracle Database...

        // After assigning the role, notify via Serverless Function
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                restTemplate.postForEntity(functionUrl, jsonEntity(payload), String.class);
                System.out.println("ms-roles: Notificación enviada asíncronamente a la función serverless.");
            } catch (Exception e) {
                System.err.println("Could not reach Azure function: " + e.getMessage());
            }
        });

        return ResponseEntity.ok("Role assigned successfully and notification sent.");
    }
}
