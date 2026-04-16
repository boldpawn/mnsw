package nl.mnsw.auth.domain

import java.util.UUID

/**
 * User — systeemgebruiker.
 * Geen Spring- of JPA-annotaties — pure domeinlaag.
 * email is de login-identifier; passwordHash is BCrypt.
 * eori wordt later door ADMIN ingevuld voor agent-partijen.
 * portLocode is verplicht voor HAVENAUTORITEIT; NULL voor andere rollen.
 */
data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val eori: String?,
    val role: Role,
    val portLocode: String?,
    val active: Boolean
)
