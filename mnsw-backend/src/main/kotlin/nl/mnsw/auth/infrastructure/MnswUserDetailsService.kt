package nl.mnsw.auth.infrastructure

import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.infrastructure.persistence.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Spring Security UserDetailsService implementatie voor MNSW.
 * Laadt gebruikers via UserRepository op basis van e-mailadres.
 * Maakt MnswUserDetails aan met rol, userId en portLocode.
 */
@Service
class MnswUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val userEntity = userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("Gebruiker niet gevonden: $username")

        return MnswUserDetails(
            userId = userEntity.id,
            email = userEntity.email,
            passwordHash = userEntity.passwordHash,
            role = userEntity.role,
            portLocode = userEntity.portLocode,
            active = userEntity.active
        )
    }
}

/**
 * Spring Security UserDetails implementatie met MNSW-specifieke velden.
 * Bevat userId, role en portLocode naast de standaard UserDetails-interface.
 * Rolnamen worden vertaald naar Spring Security ROLE_* conventies conform het autorisatiemodel.
 *
 * Roltoewijzing:
 *  SCHEEPSAGENT  -> ROLE_AGENT
 *  LADINGAGENT   -> ROLE_AGENT
 *  HAVENAUTORITEIT -> ROLE_AUTHORITY
 *  ADMIN         -> ROLE_ADMIN
 */
data class MnswUserDetails(
    val userId: UUID,
    val email: String,
    val passwordHash: String,
    val role: Role,
    val portLocode: String?,
    val active: Boolean
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val springRole = when (role) {
            Role.SCHEEPSAGENT -> "ROLE_AGENT"
            Role.LADINGAGENT -> "ROLE_AGENT"
            Role.HAVENAUTORITEIT -> "ROLE_AUTHORITY"
            Role.ADMIN -> "ROLE_ADMIN"
        }
        return listOf(SimpleGrantedAuthority(springRole))
    }

    override fun getPassword(): String = passwordHash
    override fun getUsername(): String = email
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = active
}
