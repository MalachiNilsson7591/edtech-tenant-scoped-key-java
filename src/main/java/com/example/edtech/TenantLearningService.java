package com.example.edtech;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public final class TenantLearningService {
    private final InfraiClient infrai;

    public TenantLearningService(InfraiClient infrai) { this.infrai = infrai; }

    public TenantResult provisionAndReport(Tenant tenant) throws Exception {
        String keyEnvelope = infrai.createKey(tenant.id(), tenant.name() + " delivery", "account.keys.create", tenant.id() + "-key");
        String userEnvelope = infrai.createUser(tenant.educatorEmail(), tenant.educatorPassword(), tenant.educatorName(), "tenant=" + tenant.id(), "", "default_vendor", tenant.id() + "-user");
        return new TenantResult(tenant.id(), value(keyEnvelope, "key_id"), value(userEnvelope, "user_id"), eligibleCourses(tenant.courses(), LocalDate.now()));
    }

    public static List<Course> eligibleCourses(List<Course> courses, LocalDate today) {
        return courses.stream().filter(course -> !course.deadline().isBefore(today)).collect(Collectors.toList());
    }

    private static String value(String envelope, String field) {
        int from = envelope.indexOf("\"data\"");
        if (from >= 0) {
            int objectStart = envelope.indexOf('{', from);
            if (objectStart >= 0) {
                String nested = stringValue(envelope, objectStart, field);
                if (nested != null) return nested;
            }
        }
        String result = stringValue(envelope, 0, field);
        return result == null ? "returned-data" : result;
    }

    private static String stringValue(String json, int start, String field) {
        String marker = "\"" + field + "\":\"";
        int position = json.indexOf(marker, start);
        if (position < 0) return null;
        int from = position + marker.length();
        StringBuilder value = new StringBuilder();
        for (int i = from; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == from || json.charAt(i - 1) != '\\')) return value.toString();
            value.append(c);
        }
        return null;
    }

    public record Tenant(String id, String name, String educatorEmail, String educatorPassword, String educatorName, List<Course> courses) {}
    public record Course(String title, LocalDate deadline) {}
    public record TenantResult(String tenantId, String keyId, String userId, List<Course> deliverableCourses) {}
}
