package nl.mnsw.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import nl.mnsw.auth.infrastructure.JwtService
import nl.mnsw.auth.infrastructure.MnswUserDetailsService
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT authenticatiefilter — verwerkt Bearer tokens op elk inkomend request.
 *
 * Bij een geldig JWT-token wordt het SecurityContext gevuld met een
 * UsernamePasswordAuthenticationToken zodat Spring Security de request als
 * geauthenticeerd beschouwt.
 *
 * Bij een ontbrekend of ongeldig token gaat de request door naar de filter chain.
 * Spring Security past daarna de geconfigureerde toegangsregels toe (bijv. 401/403).
 */
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: MnswUserDetailsService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        // Geen of geen Bearer token — doorgaan, Spring Security beslist over toegang
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix("Bearer ").trim()

        // Alleen verwerken als er nog geen authenticatie in de context zit
        if (SecurityContextHolder.getContext().authentication == null) {
            try {
                if (jwtService.validateToken(token)) {
                    val email = jwtService.extractEmail(token)
                    val userDetails = userDetailsService.loadUserByUsername(email)

                    if (userDetails.isEnabled) {
                        val authentication = UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.authorities
                        )
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authentication
                    }
                }
            } catch (e: Exception) {
                // Ongeldige token — log op debug-niveau en doorgaan zonder authenticatie
                log.debug("JWT validatie mislukt voor request ${request.requestURI}: ${e.message}")
            }
        }

        filterChain.doFilter(request, response)
    }
}
