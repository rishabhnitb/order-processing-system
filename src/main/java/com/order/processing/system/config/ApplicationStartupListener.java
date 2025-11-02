package com.order.processing.system.config;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener for application startup events that manages application availability states.
 * Handles the transition of application state to fully operational by setting appropriate
 * liveness and readiness states once the application is ready.
 */
@Component
public class ApplicationStartupListener {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs the startup listener with an event publisher for state management.
     *
     * @param eventPublisher The Spring event publisher for broadcasting state changes
     */
    public ApplicationStartupListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handles the ApplicationReadyEvent by setting the application's operational state.
     * This method is called automatically when Spring Boot has completed its startup process.
     * Sets both liveness (application is functioning) and readiness (application can accept traffic)
     * states to indicate the application is fully operational.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Set application as live and ready when startup is complete
        AvailabilityChangeEvent.publish(eventPublisher, this, LivenessState.CORRECT);
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
    }
}
