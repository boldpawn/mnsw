package nl.mnsw.auth.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import nl.mnsw.auth.domain.Role
import java.time.OffsetDateTime
import java.util.UUID

/**
 * JPA-entiteit voor de app_user tabel.
 * Gescheiden van de domeinlaag conform hexagonale architectuur.
 * Mapping naar domein-klasse User via UserMapper.
 *
 * Let op: @Enumerated(STRING) — PostgreSQL ENUM type wordt behandeld als VARCHAR op JDBC niveau.
 * Hibernate mapped de Kotlin enum naar de PostgreSQL ENUM string representatie.
 */
@Entity
@Table(name = "app_user")
class UserJpaEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "email", nullable = false, unique = true)
    var email: String = "",

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(name = "full_name", nullable = false)
    var fullName: String = "",

    @Column(name = "eori", length = 17)
    var eori: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "user_role")
    var role: Role = Role.SCHEEPSAGENT,

    @Column(name = "port_locode", length = 10)
    var portLocode: String? = null,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)
