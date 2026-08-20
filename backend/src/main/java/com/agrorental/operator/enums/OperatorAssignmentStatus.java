package com.agrorental.operator.enums;

/**
 * Lifecycle status of an operator's job assignment to a machinery booking.
 */
public enum OperatorAssignmentStatus {
    /** Operator has been actively assigned by Partner or Admin; awaiting operator decision. */
    ASSIGNED,

    /** Operator has accepted the job assignment. */
    ACCEPTED,

    /** Operator declined the job assignment with a stated reason. */
    REJECTED,

    /** Operator is en route to the farm / machinery service location. */
    TRAVELING,

    /** Operator has arrived at the farm / fieldwork destination. */
    REACHED,

    /** Fieldwork / machinery operation is actively underway. */
    IN_PROGRESS,

    /** Fieldwork has been temporarily paused (weather, maintenance, break, etc.). */
    PAUSED,

    /** Fieldwork has been successfully concluded. */
    COMPLETED,

    /** Assignment cancelled prior to job commencement. */
    CANCELLED
}
