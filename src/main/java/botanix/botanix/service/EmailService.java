package botanix.botanix.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void enviarCorreoRecuperacion(String destinatario, String enlace) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(destinatario);
            helper.setSubject("Restablecer tu contraseña - Botanix");
            
            String contenidoHtml = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #DEF4C6; border-radius: 10px;\">"
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
            
            helper.setText(contenidoHtml, true);
            mailSender.send(message);
            System.out.println("Correo de recuperación enviado con éxito a: " + destinatario);
        } catch (Exception e) {
            System.err.println("Error al enviar el correo a " + destinatario + ": " + e.getMessage());
        }
    }
}
