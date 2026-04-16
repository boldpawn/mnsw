package nl.mnsw.auth.domain

/**
 * Gebruikersrol in het MNSW systeem.
 * Bepaalt toegang en rechten conform het autorisatiemodel.
 */
enum class Role {
    SCHEEPSAGENT,    // Indienen en inzien eigen formalities
    LADINGAGENT,     // Indienen en inzien eigen formalities
    HAVENAUTORITEIT, // Inzien en beoordelen voor eigen haven
    ADMIN            // Volledige toegang
}
