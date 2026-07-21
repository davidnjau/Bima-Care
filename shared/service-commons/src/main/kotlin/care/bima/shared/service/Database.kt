package care.bima.shared.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

private const val DEFAULT_MAX_POOL_SIZE = 10

fun connectToPostgres(
    jdbcUrl: String = System.getenv("DB_URL") ?: error("DB_URL environment variable is required"),
    username: String = System.getenv("DB_USER") ?: error("DB_USER environment variable is required"),
    password: String = System.getenv("DB_PASSWORD") ?: error("DB_PASSWORD environment variable is required"),
): Database {
    val config =
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = DEFAULT_MAX_POOL_SIZE
        }
    return Database.connect(HikariDataSource(config))
}
