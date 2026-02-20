package com.frauddetection.frauddecision.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalised fraud decision threshold configuration.
 *
 * SOLID Fix: OCP — thresholds are no longer hardcoded magic numbers.
 *
 * Usage in application.yml:
 * 
 * <pre>
 * fraud:
 *   decision:
 *     block-threshold: 0.8
 *     review-threshold: 0.6
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "fraud.decision")
@Data
public class DecisionProperties {

    /** Risk scores above this threshold result in an automatic BLOCK. */
    private double blockThreshold = 0.80;

    /** Risk scores above this threshold (but below block) require manual REVIEW. */
    private double reviewThreshold = 0.60;
}
