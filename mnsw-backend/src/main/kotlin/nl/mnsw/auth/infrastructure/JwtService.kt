package nl.mnsw.auth.infrastructure

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.domain.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

/**
 * JWT-service voor het genereren en valideren van tokens.
 * Gebruikt JJWT 0.12.x API.
 *
 * Claims layout:
 *  - sub: userId (UUID als String)
 *  - email: e-mailadres
 *  - role: Rol als String (SCHEEPSAGENT, LADINGAGENT, HAVENAUTORITEIT, ADMIN)
 *  - portLocode: havencodes voor HAVENAUTORITEIT (null voor andere rollen)
 *  - iat: issued at
 *  - exp: expiration
 */
@Service
class JwtService(
    @Value("\${security.jwt.secret}") private val secret: String,
    @Value("\${security.jwt.expiration-hours}") private val expirationHours: Long
) {

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))
    }

    /**
     * Genereer een gesigneerd JWT-token voor een gebruiker.
     * Bevat userId, email, rol en optioneel portLocode als claims.
     */
    fun generateToken(user: User): String {
        val now = Date()
        val expiration = Date(now.time + expirationHours * 3600 * 1000)

        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .claim("portLocode", user.portLocode)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(signingKey)
            .compact()
    }

    /**
     * Valideer een JWT-token. Retourneert false bij een ongeldige of verlopen token.
     */
    fun validateToken(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Haal de userId uit een token.
     * @throws JwtException als de token ongeldig is.
     */
    fun extractUserId(token: String): UUID {
        val subject = parseClaims(token).subject
        return UUID.fromString(subject)
    }

    /**
     * Haal de rol uit een token.
     * @throws JwtException als de token ongeldig is.
     */
    fun extractRole(token: String): Role {
        val roleName = parseClaims(token).get("role", String::class.java)
        return Role.valueOf(roleName)
    }

    /**
     * Haal de portLocode uit een token (null voor niet-HAVENAUTORITEIT).
     */
    fun extractPortLocode(token: String): String? {
        return parseClaims(token).get("portLocode", String::class.java)
    }

    /**
     * Haal het e-mailadres uit een token.
     * @throws JwtException als de token ongeldig is.
     */
    fun extractEmail(token: String): String {
        return parseClaims(token).get("email", String::class.java)
    }

    /**
     * Bereken de verloopdatum van een token.
     */
    fun extractExpiration(token: String): OffsetDateTime {
        val expDate = parseClaims(token).expiration
        return expDate.toInstant().atOffset(java.time.ZoneOffset.UTC)
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
