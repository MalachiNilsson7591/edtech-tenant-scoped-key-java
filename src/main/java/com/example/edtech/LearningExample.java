package com.example.edtech;

import java.time.LocalDate;
import java.util.List;

public final class LearningExample {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        String recipient = System.getenv("LEARNING_EXAMPLE_EMAIL");
        if (recipient == null || recipient.isBlank()) throw new IllegalStateException("LEARNING_EXAMPLE_EMAIL is required");
        TenantLearningService.Tenant tenant = new TenantLearningService.Tenant("north-campus", "North Campus", recipient, "change-me-before-live-run", "Asha Patel", List.of(
                new TenantLearningService.Course("Algebra foundations", LocalDate.now().plusDays(7)),
                new TenantLearningService.Course("History review", LocalDate.now().minusDays(1))));
        InfraiClient client = new InfraiClient(key);
        TenantLearningService.TenantResult result = new TenantLearningService(client).provisionAndReport(tenant);
        System.out.println("tenant=" + result.tenantId() + " educator=" + result.userId() + " deliverable=" + result.deliverableCourses().stream().map(TenantLearningService.Course::title).toList());
        client.deleteUser(result.userId());
        client.revokeKey(result.keyId());
        if (client.listKeys().contains("\"" + result.keyId() + "\"")) {
            throw new IllegalStateException("key cleanup could not be verified");
        }
        System.out.println("cleanup=verified");
    }
}
