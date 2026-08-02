package botanix.botanix.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private String port;

    @Value("${spring.mail.username}")
    private String remitente;

    @Value("${spring.mail.password}")
    private String password;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void verificarConfiguracion() {
        boolean passOk = password != null && !password.isBlank();
        log.info("EmailService iniciado. SMTP {}:{} remitente={} password={}",
                host == null ? "?" : host,
                port == null ? "?" : port,
                remitente == null || remitente.isBlank() ? "(no configurado)" : remitente,
                passOk ? "(configurada)" : "(VACIA)");
        if (remitente == null || remitente.isBlank()) {
            log.warn("spring.mail.username no esta configurado: el envio de correos fallara.");
        }
        if (!passOk) {
            log.warn("spring.mail.password no esta configurado (variable MAIL_PASSWORD): la autenticacion fallara.");
        }
    }

    public void enviarCorreoRecuperacion(String destinatario, String enlace) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(remitente);
        helper.setTo(destinatario);
        helper.setSubject("Restablecer tu contraseña - Botanix");
        helper.setText(construirContenidoRecuperacion(enlace), true);

        mailSender.send(message);
        log.info("Correo de recuperación enviado a {}. MessageId={}", destinatario, message.getMessageID());
    }

    private String construirContenidoRecuperacion(String enlace) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #DEF4C6; border-radius: 10px;\">"
                + "<h2 style=\"color: #1B512D;\">Restablecer tu contraseña</h2>"
                + "<p>Hola,</p>"
                + "<p>Has solicitado restablecer tu contraseña en el sistema de gestión <strong>Botanix</strong>.</p>"
                + "<p>Haz clic en el siguiente botón para establecer una nueva contraseña. Este enlace expira en 1 hora:</p>"
                + "<p style=\"text-align: center; margin: 30px 0;\">"
                + "  <a href=\"" + enlace + "\" style=\"display: inline-block; padding: 12px 24px; color: white; background-color: #1C7C54; border-radius: 8px; text-decoration: none; font-weight: bold;\">Restablecer Contraseña</a>"
                + "</p>"
                + "<p>Si no solicitaste este cambio, puedes ignorar este correo de forma segura.</p>"
                + "<br>"
                + "<p>Atentamente,<br>El equipo de Botanix</p>"
                + "</div>";
    }
}
