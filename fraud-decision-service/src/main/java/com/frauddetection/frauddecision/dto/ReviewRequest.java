package com.frauddetection.frauddecision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for the analyst case-review endpoint.
 *
 * SOLID Fix: Replaces Map<String, String> body with a typed, validated DTO.
 * ─────────────────────────────────────────────────────────────────────────────
 * BEFORE: @RequestBody Map<String, String> body — no validation, any string
 * accepted,
 * controller manually called body.get("action") with no type safety.
 *
 * AFTER: Typed DTO with @Valid — invalid actions are rejected at the framework
 * level with a 400 response before the controller method is even called.
 *
 * Pattern: DTO (Data Transfer Object) + Bean Validation
 */
@Data
public class ReviewRequest {

    @NotBlank(message = "action is required")
    @Pattern(regexp = "APPROVE|REJECT", message = "action must be APPROVE or REJECT")
    private String action;
}
