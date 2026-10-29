package io.github.tuyucheng777.quarkforge.observability;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/// ## FrameworkHealthCheck
/// 框架存活探针，挂载于 `/q/health/live`。
@Liveness
@ApplicationScoped
public class FrameworkHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("quarkforge");
    }
}
