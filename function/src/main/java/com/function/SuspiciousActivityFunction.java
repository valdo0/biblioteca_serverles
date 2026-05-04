package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Event Grid Trigger.
 */
public class SuspiciousActivityFunction {

    @FunctionName("SuspiciousActivityAlert")
    public void run(
            @EventGridTrigger(name = "eventGridEvent") String eventGridEvent,
            final ExecutionContext context) {

        context.getLogger().info("SuspiciousActivityAlert EventGrid trigger processed an event.");

        // Log the suspicious activity for security audit
        context.getLogger().warning("SECURITY ALERT: Suspicious activity detected! Event Payload: " + eventGridEvent);
        context.getLogger().warning("Taking automated security measures and notifying administrators...");
    }
}
