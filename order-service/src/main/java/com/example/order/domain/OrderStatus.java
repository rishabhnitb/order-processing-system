package com.example.order.domain;

import java.util.Set;

/**
 * State machine for order lifecycle.
 *
 * Each constant declares its own allowed transitions — this is the State Pattern.
 * The service layer calls canTransitionTo() instead of having if-else chains.
 *
 * Valid transitions:
 *   PENDING    → PROCESSING, CANCELLED
 *   PROCESSING → SHIPPED
 *   SHIPPED    → DELIVERED
 *   DELIVERED  → (terminal — no further transitions)
 *   CANCELLED  → (terminal — no further transitions)
 */
public enum OrderStatus {

    PENDING {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(PROCESSING, CANCELLED);
        }
    },
    PROCESSING {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(SHIPPED);
        }
    },
    SHIPPED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(DELIVERED);
        }
    },
    DELIVERED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of();
        }
    },
    CANCELLED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of();
        }
    };

    /** Each constant defines which statuses it can move to. */
    public abstract Set<OrderStatus> allowedTransitions();

    /** Called by service layer to validate a transition before attempting it. */
    public boolean canTransitionTo(OrderStatus next) {
        return allowedTransitions().contains(next);
    }
}
