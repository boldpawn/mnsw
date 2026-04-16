package nl.mnsw.formality.domain

/**
 * Indieningskanaal van een formality.
 * WEB = via Angular frontend
 * RIM = via Remote Interface Module (AS4/eDelivery via Apache Pulsar)
 */
enum class SubmissionChannel {
    WEB,
    RIM
}
