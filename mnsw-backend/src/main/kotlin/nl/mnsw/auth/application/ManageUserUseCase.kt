package nl.mnsw.auth.application

import nl.mnsw.auth.domain.Role
import nl.mnsw.auth.domain.User
import nl.mnsw.auth.infrastructure.persistence.UserJpaEntity
import nl.mnsw.auth.infrastructure.persistence.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Use case voor gebruikersbeheer door ADMIN.
 * Biedt CRUD-operaties op gebruikers conform api.md.
 * Soft delete via active = false.
 */
@Service
@Transactional
class ManageUserUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional(readOnly = true)
    fun listUsers(pageable: Pageable): Page<User> {
        return userRepository.findAll(pageable).map { entity ->
            entity.toDomain()
        }
    }

    fun createUser(command: CreateUserCommand): User {
        if (userRepository.existsByEmail(command.email)) {
            throw UserAlreadyExistsException(command.email)
        }

        val entity = UserJpaEntity(
            id = UUID.randomUUID(),
            email = command.email,
            passwordHash = passwordEncoder.encode(command.password),
            fullName = command.fullName,
            role = command.role,
            portLocode = command.portLocode,
            active = true
        )

        return userRepository.save(entity).toDomain()
    }

    fun updateUser(userId: UUID, command: UpdateUserCommand): User {
        val entity = userRepository.findById(userId).orElseThrow {
            UserNotFoundException(userId)
        }

        command.fullName?.let { entity.fullName = it }
        command.role?.let { entity.role = it }
        command.portLocode?.let { entity.portLocode = it }
        command.active?.let { entity.active = it }
        command.password?.let { entity.passwordHash = passwordEncoder.encode(it) }

        return userRepository.save(entity).toDomain()
    }

    fun deactivateUser(userId: UUID) {
        val entity = userRepository.findById(userId).orElseThrow {
            UserNotFoundException(userId)
        }
        entity.active = false
        userRepository.save(entity)
    }

    private fun UserJpaEntity.toDomain() = User(
        id = id,
        email = email,
        passwordHash = passwordHash,
        fullName = fullName,
        eori = eori,
        role = role,
        portLocode = portLocode,
        active = active
    )
}

data class CreateUserCommand(
    val email: String,
    val password: String,
    val fullName: String,
    val role: Role,
    val portLocode: String?
)

data class UpdateUserCommand(
    val fullName: String?,
    val role: Role?,
    val portLocode: String?,
    val active: Boolean?,
    val password: String?
)

class UserAlreadyExistsException(email: String) : RuntimeException("Gebruiker bestaat al: $email")
class UserNotFoundException(userId: UUID) : RuntimeException("Gebruiker niet gevonden: $userId")
