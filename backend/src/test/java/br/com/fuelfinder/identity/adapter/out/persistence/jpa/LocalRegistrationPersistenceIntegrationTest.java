package br.com.fuelfinder.identity.adapter.out.persistence.jpa;

import br.com.fuelfinder.identity.application.model.PasswordHash;
import br.com.fuelfinder.identity.domain.model.User;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class LocalRegistrationPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(
                    DockerImageName
                            .parse("postgis/postgis:17-3.5")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("fuelfinder_integration")
                    .withUsername("fuelfinder_test")
                    .withPassword("integration-test-only");

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );
        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver"
        );
        registry.add("spring.flyway.enabled", () -> true);
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
    }

    @Autowired
    private LocalRegistrationPersistenceAdapter registration;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldApplyFlywayMigrationV1() {
        Long count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '1'
                  AND success = true
                """,
                Long.class
        );

        assertEquals(1L, count);
    }

    @Test
    void shouldPersistEntireRegistration() {
        User user = newUser();

        registration.register(
                user,
                new PasswordHash("integration-test-hash")
        );

        assertRecordCounts(user.getId().value(), 1, 1, 1);

        String status = jdbc.queryForObject(
                """
                SELECT status
                FROM identity_verification_email_outbox
                WHERE user_id = ?
                """,
                String.class,
                user.getId().value()
        );

        assertEquals("PENDING", status);
    }

    @Test
    void shouldRollbackWhenOutboxInsertFails() {
        User user = newUser();

        // Falha forçada apenas no banco descartavel de teste.
        jdbc.execute("""
                CREATE FUNCTION fail_outbox_insert_test()
                RETURNS trigger AS $$
                BEGIN
                    RAISE EXCEPTION 'forced outbox failure';
                END;
                $$ LANGUAGE plpgsql
                """);

        jdbc.execute("""
                CREATE TRIGGER fail_outbox_insert_test_trigger
                BEFORE INSERT
                ON identity_verification_email_outbox
                FOR EACH ROW
                EXECUTE FUNCTION fail_outbox_insert_test()
                """);

        try {
            assertThrows(
                    RuntimeException.class,
                    () -> registration.register(
                            user,
                            new PasswordHash("integration-test-hash")
                    )
            );
        } finally {
            jdbc.execute("""
                    DROP TRIGGER IF EXISTS
                    fail_outbox_insert_test_trigger
                    ON identity_verification_email_outbox
                    """);

            jdbc.execute("""
                    DROP FUNCTION IF EXISTS
                    fail_outbox_insert_test()
                    """);
        }

        // Nenhuma das tres tabelas pode conter
        // registros pertencentes ao cadastro falho.
        assertRecordCounts(user.getId().value(), 0, 0, 0);
    }

    @Test
    void shouldRejectDuplicateEmailInDatabase() {
        String email = "test-" + UUID.randomUUID() + "@example.com";

        User first = newUserWithEmail(email);
        User second = newUserWithEmail(email);

        registration.register(
                first,
                new PasswordHash("first-encoded-hash")
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> registration.register(
                        second,
                        new PasswordHash("second-encoded-hash")
                )
        );

        assertRecordCounts(first.getId().value(), 1, 1, 1);
        assertRecordCounts(second.getId().value(), 0, 0, 0);
    }

    private User newUser() {
        return newUserWithEmail(
                "test-" + UUID.randomUUID() + "@example.com"
        );
    }

    private User newUserWithEmail(String email) {
        return User.registerLocal(
                UserId.newId(),
                "Maria Silva",
                Email.of(email),
                Instant.now()
        );
    }

    private void assertRecordCounts(
            UUID userId,
            long expectedUsers,
            long expectedCredentials,
            long expectedEvents
    ) {
        assertEquals(
                expectedUsers,
                count(
                        "SELECT COUNT(*) FROM identity_users WHERE id = ?",
                        userId
                )
        );

        assertEquals(
                expectedCredentials,
                count(
                        "SELECT COUNT(*) FROM identity_local_credentials WHERE user_id = ?",
                        userId
                )
        );

        assertEquals(
                expectedEvents,
                count(
                        "SELECT COUNT(*) FROM identity_verification_email_outbox WHERE user_id = ?",
                        userId
                )
        );
    }

    private long count(String sql, UUID userId) {
        Long result = jdbc.queryForObject(
                sql,
                Long.class,
                userId
        );

        return result == null ? 0 : result;
    }
}
