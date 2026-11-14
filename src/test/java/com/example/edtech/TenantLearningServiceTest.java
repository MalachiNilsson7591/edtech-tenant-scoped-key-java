package com.example.edtech;

import java.time.LocalDate;
import java.util.List;

public final class TenantLearningServiceTest {
    public static void main(String[] args) {
        LocalDate today = LocalDate.of(2026, 10, 24);
        List<TenantLearningService.Course> result = TenantLearningService.eligibleCourses(List.of(
                new TenantLearningService.Course("Open lesson", today),
                new TenantLearningService.Course("Closed lesson", today.minusDays(1))), today);
        if (result.size() != 1 || !result.get(0).title().equals("Open lesson")) throw new AssertionError("deadline decision changed");
        System.out.println("deadline decision passed");
    }
}
