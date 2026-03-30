package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class RoleNotificationFunction {

    @FunctionName("RoleNotification")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "notify/role") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("RoleNotificationFunction HTTP trigger processed a request.");

        // Parse query parameter or body
        String body = request.getBody().orElse("{}");

        // Log the role modification for audit/notification purposes
        context.getLogger().info("Role Modification Detected. Payload: " + body);
        context.getLogger().info("Sending Email/SMS Notification to User regarding Role change...");

        return request.createResponseBuilder(HttpStatus.OK)
                .body("Role notification processed successfully.")
                .build();
    }
}
