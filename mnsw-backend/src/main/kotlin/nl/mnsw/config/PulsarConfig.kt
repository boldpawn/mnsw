package nl.mnsw.config

import org.springframework.context.annotation.Configuration

/**
 * Pulsar configuratie: topic-namen als constanten.
 * Topics worden automatisch aangemaakt bij eerste gebruik (Pulsar autocreate).
 *
 * Naamgevingsconventie: persistent://public/default/mnsw-{richting}-{type}
 * Retentie en partities worden geconfigureerd via Pulsar admin (buiten applicatiescope).
 */
@Configuration
class PulsarConfig {

    companion object {
        /** Inbound topics — berichten van RIM naar MNSW, per formality type */
        const val TOPIC_RIM_INBOUND_NOA = "persistent://public/default/mnsw-rim-inbound-noa"
        const val TOPIC_RIM_INBOUND_NOS = "persistent://public/default/mnsw-rim-inbound-nos"
        const val TOPIC_RIM_INBOUND_NOD = "persistent://public/default/mnsw-rim-inbound-nod"
        const val TOPIC_RIM_INBOUND_VID = "persistent://public/default/mnsw-rim-inbound-vid"
        const val TOPIC_RIM_INBOUND_SID = "persistent://public/default/mnsw-rim-inbound-sid"

        /** Intern topic — gepubliceerd na succesvolle opslag van een formality */
        const val TOPIC_FORMALITIES_SUBMITTED = "persistent://public/default/mnsw-formalities-submitted"

        /** Outbound topic — FRM-responses terug naar RIM-indieners */
        const val TOPIC_FRM_OUTBOUND = "persistent://public/default/mnsw-frm-outbound"
    }
}
