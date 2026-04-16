package nl.mnsw.formality.domain

/**
 * Type van een EMSWe formality.
 * Conform EMSA MIG v2.0.1 — inkomende berichttypes.
 * FRM is een uitkomend antwoord en staat hier NIET in.
 */
enum class FormalityType {
    NOA,  // Notification of Arrival
    NOS,  // Notification of Sailing
    NOD,  // Notification of Departure
    VID,  // Vessel Identification Documents
    SID   // Ship/Security Information Document (ISPS)
}
