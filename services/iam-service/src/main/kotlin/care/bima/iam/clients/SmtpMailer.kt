package care.bima.iam.clients

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import java.util.Properties

private val logger = LoggerFactory.getLogger(SmtpMailer::class.java)
private const val DEFAULT_SMTP_PORT = 587

/**
 * The only outbound-email need right now is "deliver a freshly-provisioned account's temp
 * password" (see MemberAccountProvisioningConsumer/InsurerAccountProvisioningConsumer) - a
 * dedicated Notification service (BIMA CARE.md §4) is overkill for one call site, so this
 * lives directly in iam-service. A send failure is never fatal - the account is already
 * created and usable by the time this runs, so a delivery hiccup shouldn't look like a
 * provisioning failure. Callers fall back to logging the password when this returns false.
 */
class SmtpMailer(
    private val host: String = System.getenv("SMTP_HOST") ?: "",
    private val port: Int = System.getenv("SMTP_PORT")?.toIntOrNull() ?: DEFAULT_SMTP_PORT,
    private val username: String = System.getenv("SMTP_USERNAME") ?: "",
    private val password: String = System.getenv("SMTP_PASSWORD") ?: "",
    private val fromAddress: String = System.getenv("SMTP_FROM_ADDRESS") ?: username,
) {
    private val enabled = host.isNotBlank() && username.isNotBlank()

    fun sendTempPasswordEmail(
        to: String,
        recipientName: String,
        loginUsername: String,
        tempPassword: String,
    ): Boolean {
        if (!enabled) return false
        return runCatching {
            Transport.send(buildMessage(to, recipientName, loginUsername, tempPassword))
            true
        }.getOrElse {
            logger.warn("Failed to send temp-password email to $to", it)
            false
        }
    }

    private fun buildMessage(
        to: String,
        recipientName: String,
        loginUsername: String,
        tempPassword: String,
    ): MimeMessage {
        val properties =
            Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", host)
                put("mail.smtp.port", port.toString())
            }
        val session =
            Session.getInstance(
                properties,
                object : Authenticator() {
                    override fun getPasswordAuthentication() = PasswordAuthentication(username, password)
                },
            )
        return MimeMessage(session).apply {
            setFrom(InternetAddress(fromAddress))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            subject = "Your BimaCare account"
            setText(
                "Hello $recipientName,\n\n" +
                    "An account has been created for you on BimaCare.\n\n" +
                    "Username: $loginUsername\n" +
                    "Temporary password: $tempPassword\n\n" +
                    "Please log in and keep this password safe.",
            )
        }
    }
}
