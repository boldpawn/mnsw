package nl.mnsw.formality.domain

/**
 * Lifecycle status van een formality.
 *
 * Statusovergangen:
 *   SUBMITTED -> UNDER_REVIEW (havenautoriteit opent)
 *   SUBMITTED -> REJECTED     (directe afwijzing bij validatiefout)
 *   UNDER_REVIEW -> ACCEPTED
 *   UNDER_REVIEW -> REJECTED
 *   ACCEPTED/REJECTED/UNDER_REVIEW -> SUPERSEDED (bij correctie door indiener)
 */
enum class FormalityStatus {
    SUBMITTED,
    UNDER_REVIEW,
    ACCEPTED,
    REJECTED,
    SUPERSEDED
}
