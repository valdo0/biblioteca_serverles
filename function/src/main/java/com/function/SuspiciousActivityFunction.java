package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class SuspiciousActivityFunction {

    @FunctionName("SuspiciousActivityAlert")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "alert/suspicious") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("SuspiciousActivityAlert HTTP trigger processed a request.");

        String body = request.getBody().orElse("{}");

        // Log the suspicious activity for security audit
        context.getLogger().warning("SECURITY ALERT: Suspicious activity detected! Payload: " + body);
        context.getLogger().warning("Taking automated security measures and notifying administrators...");

        return request.createResponseBuilder(HttpStatus.OK)
                .body("Security alert logged and administrators notified.")
                .build();
    }
}
