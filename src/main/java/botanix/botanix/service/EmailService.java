package botanix.botanix.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final RestClient restClient;

    @Value("${mail.api.url}")
    private String apiUrl;

    @Value("${mail.api.key}")
    private String apiKey;

    @Value("${mail.from}")
    private String remitente;

    @Value("${mail.from.name}")
    private String remitenteNombre;

    public EmailService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @PostConstruct
    public void verificarConfiguracion() {
        boolean keyOk = apiKey != null && !apiKey.isBlank();
        log.info("EmailService (Brevo) iniciado. remitente={} apiKey={}",
                remitente == null || remitente.isBlank() ? "(no configurado)" : remitente,
                keyOk ? "(configurada)" : "(VACIA)");
        if (!keyOk) {
            log.warn("MAIL_API_KEY no esta configurada: el envio de correos fallara.");
        }
    }

    public void enviarCorreoRecuperacion(String destinatario, String enlace) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("MAIL_API_KEY no esta configurada.");
        }

        Map<String, Object> cuerpo = Map.of(
                "sender", Map.of("email", remitente, "name", remitenteNombre),
                "to", List.of(Map.of("email", destinatario)),
                "subject", "Restablecer tu contraseña - Botanix",
                "htmlContent", construirContenidoRecuperacion(enlace)
        );

        String respuesta = restClient.post()
                .uri(apiUrl)
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(String.class);

        log.info("Correo de recuperación enviado a {}. Respuesta de Brevo: {}", destinatario, respuesta);
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
