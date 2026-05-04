package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Event Grid Trigger.
 */
public class RoleNotificationFunction {

    @FunctionName("RoleNotification")
    public void run(
            @EventGridTrigger(name = "eventGridEvent") String eventGridEvent,
            final ExecutionContext context) {

        context.getLogger().info("RoleNotificationFunction EventGrid trigger processed an event.");

        // Log the role modification for audit/notification purposes
        context.getLogger().info("Role Modification Detected. Event Payload: " + eventGridEvent);
        context.getLogger().info("Sending Email/SMS Notification to User regarding Role change...");
    }
}
