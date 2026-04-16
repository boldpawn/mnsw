package nl.mnsw.auth.infrastructure.web

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import nl.mnsw.auth.application.CreateUserCommand
import nl.mnsw.auth.application.ManageUserUseCase
import nl.mnsw.auth.application.UpdateUserCommand
import nl.mnsw.auth.domain.Role
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * REST controller voor gebruikersbeheer.
 * Alle endpoints vereisen ADMIN-rol (@PreAuthorize op klasse-niveau).
 *
 * GET  /api/v1/users       — lijst van gebruikers (gepagineerd)
 * POST /api/v1/users       — nieuwe gebruiker aanmaken
 * PUT  /api/v1/users/{id}  — gebruiker bewerken
 * DELETE /api/v1/users/{id} — gebruiker deactiveren (soft delete)
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
class UserController(
    private val manageUserUseCase: ManageUserUseCase
) {

    @GetMapping
    fun list(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<UserDto>> {
        val users = manageUserUseCase.listUsers(pageable)
        return ResponseEntity.ok(users.map { user ->
            UserDto(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
                role = user.role,
                portLocode = user.portLocode,
                active = user.active
            )
        })
    }

    @PostMapping
    fun create(
        @RequestBody @Valid request: CreateUserRequest
    ): ResponseEntity<UserDto> {
        val user = manageUserUseCase.createUser(
            CreateUserCommand(
                email = request.email,
                password = request.password,
                fullName = request.fullName,
                role = request.role,
                portLocode = request.portLocode
            )
        )

        return ResponseEntity.status(201).body(
            UserDto(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
                role = user.role,
                portLocode = user.portLocode,
                active = user.active
            )
        )
    }

    @PutMapping("/{userId}")
    fun update(
        @PathVariable userId: UUID,
        @RequestBody @Valid request: UpdateUserRequest
    ): ResponseEntity<UserDto> {
        val user = manageUserUseCase.updateUser(
            userId = userId,
            command = UpdateUserCommand(
                fullName = request.fullName,
                role = request.role,
                portLocode = request.portLocode,
                active = request.active,
                password = request.password
            )
        )

        return ResponseEntity.ok(
            UserDto(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
                role = user.role,
                portLocode = user.portLocode,
                active = user.active
            )
        )
    }

    @DeleteMapping("/{userId}")
    fun delete(@PathVariable userId: UUID): ResponseEntity<Void> {
        manageUserUseCase.deactivateUser(userId)
        return ResponseEntity.noContent().build()
    }
}

data class CreateUserRequest(
    @field:NotBlank(message = "Email is verplicht")
    @field:Email(message = "Geen geldig e-mailadres")
    val email: String = "",

    @field:NotBlank(message = "Wachtwoord is verplicht")
    val password: String = "",

    @field:NotBlank(message = "Volledige naam is verplicht")
    val fullName: String = "",

    @field:NotNull(message = "Rol is verplicht")
    val role: Role = Role.SCHEEPSAGENT,

    val portLocode: String? = null
)

data class UpdateUserRequest(
    val fullName: String? = null,
    val role: Role? = null,
    val portLocode: String? = null,
    val active: Boolean? = null,
    val password: String? = null
)

data class UserDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: Role,
    val portLocode: String?,
    val active: Boolean
)
