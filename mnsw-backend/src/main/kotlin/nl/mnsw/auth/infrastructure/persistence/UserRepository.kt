package nl.mnsw.auth.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository voor UserJpaEntity.
 *
 * Email is de gebruikersnaam in MNSW — uniek per gebruiker.
 * findByEmail wordt gebruikt door Spring Security tijdens authenticatie.
 * existsByEmail wordt gebruikt bij gebruikersregistratie (duplicaatcontrole).
 */
@Repository
interface UserRepository : JpaRepository<UserJpaEntity, UUID> {

    /**
     * Zoek een gebruiker op e-mailadres.
     * Gebruikt door Spring Security UserDetailsService voor authenticatie.
     * Retourneert null als de gebruiker niet bestaat.
     */
    fun findByEmail(email: String): UserJpaEntity?

    /**
     * Controleer of een e-mailadres al in gebruik is.
     * Gebruikt bij gebruikersregistratie voor duplicaatdetectie.
     */
    fun existsByEmail(email: String): Boolean
}
