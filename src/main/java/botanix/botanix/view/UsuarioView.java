package botanix.botanix.view;

import botanix.botanix.model.Usuario;
import botanix.botanix.model.PasswordResetToken;
import botanix.botanix.repository.UsuarioRepository;
import botanix.botanix.repository.PasswordResetTokenRepository;
import botanix.botanix.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
public class UsuarioView {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    @Autowired
    private EmailService emailService;

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/dashboard";
        }

        return "usuario/login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo, @RequestParam String contrasena, HttpSession session, RedirectAttributes ra) {

        Optional<Usuario> resultado = usuarioRepository.findByCorreo(correo.trim());

        if (resultado.isPresent()) {
            Usuario usuario = resultado.get();
            if (passwordEncoder.matches(contrasena,  usuario.getContrasena())) {
                session.setAttribute("usuarioLogueado", usuario.getNombre());
                session.setAttribute("usuarioId", usuario.getId_usuario());
                session.setAttribute("usuarioCorreo", usuario.getCorreo());
                return "redirect:/dashboard";
            }
        }

        ra.addFlashAttribute("error", "Correo o contrasena incorrectos");
        return "redirect:/login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(HttpSession session) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/dashboard";
        }

        return "usuario/registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@RequestParam String nombre,
                                   @RequestParam String correo,
                                   @RequestParam String contrasena,
                                   @RequestParam String confirmarContrasena,
                                   RedirectAttributes ra) {
        String nombreNormalizado = nombre.trim();
        String correoNormalizado = correo.trim();

        if (nombreNormalizado.isEmpty() || correoNormalizado.isEmpty() || contrasena.isBlank()) {
            ra.addFlashAttribute("error", "Completa todos los campos requeridos");
            return "redirect:/registro";
        }

        if (!contrasena.equals(confirmarContrasena)) {
            ra.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/registro";
        }

        if (contrasena.length() < 6) {
            ra.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres");
            return "redirect:/registro";
        }

        if (usuarioRepository.findByCorreo(correoNormalizado).isPresent()) {
            ra.addFlashAttribute("error", "Ya existe un usuario con ese correo");
            return "redirect:/registro";
        }

        Usuario usuario = Usuario.builder()
                .nombre(nombreNormalizado)
                .correo(correoNormalizado)
                .contrasena(passwordEncoder.encode(contrasena))
                .build();

        usuarioRepository.save(usuario);
        ra.addFlashAttribute("success", "Usuario registrado. Ahora puedes iniciar sesion");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/recuperar-password")
    public String mostrarRecuperarPassword(HttpSession session) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/dashboard";
        }
        return "usuario/recuperar-password";
    }

    @PostMapping("/recuperar-password")
    public String procesarRecuperarPassword(@RequestParam String correo, HttpServletRequest request, RedirectAttributes ra) {
        String correoNormalizado = correo.trim();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correoNormalizado);

        if (usuarioOpt.isEmpty()) {
            ra.addFlashAttribute("error", "No existe ningún usuario registrado con ese correo.");
            return "redirect:/recuperar-password";
        }

        Usuario usuario = usuarioOpt.get();

        // Generar un token único
        String token = UUID.randomUUID().toString();
        
        // Crear el token con expiración de 1 hora
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .usuario(usuario)
                .expiracion(LocalDateTime.now().plusHours(1))
                .usado(false)
                .build();

        tokenRepository.save(resetToken);

        // Construir el enlace
        String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        String enlace = appUrl + "/nueva-password?token=" + token;

        // Enviar el correo
        emailService.enviarCorreoRecuperacion(correoNormalizado, enlace);

        ra.addFlashAttribute("success", "Hemos enviado un correo para restablecer tu contraseña. Por favor, revisa tu bandeja de entrada.");
        return "redirect:/recuperar-password";
    }

    @GetMapping("/nueva-password")
    public String mostrarNuevaPassword(@RequestParam(required = false) String token, org.springframework.ui.Model model, RedirectAttributes ra) {
        if (token == null || token.isBlank()) {
            ra.addFlashAttribute("error", "El token de recuperación es requerido.");
            return "redirect:/login";
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            ra.addFlashAttribute("error", "El token no es válido.");
            return "redirect:/login";
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isUsado()) {
            ra.addFlashAttribute("error", "Este token ya ha sido utilizado. Solicita uno nuevo.");
            return "redirect:/recuperar-password";
        }

        if (resetToken.getExpiracion().isBefore(LocalDateTime.now())) {
            ra.addFlashAttribute("error", "El token ha expirado. Solicita uno nuevo.");
            return "redirect:/recuperar-password";
        }

        model.addAttribute("token", token);
        return "usuario/nueva-password";
    }

    @PostMapping("/nueva-password")
    public String procesarNuevaPassword(@RequestParam String token,
                                       @RequestParam String contrasena,
                                       @RequestParam String confirmarContrasena,
                                       RedirectAttributes ra) {
        if (token == null || token.isBlank()) {
            ra.addFlashAttribute("error", "Token de recuperación no válido o inexistente.");
            return "redirect:/login";
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Token de recuperación no válido o inexistente.");
            return "redirect:/login";
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isUsado() || resetToken.getExpiracion().isBefore(LocalDateTime.now())) {
            ra.addFlashAttribute("error", "El token de recuperación ha expirado o ya fue utilizado.");
            return "redirect:/recuperar-password";
        }

        if (contrasena.isBlank() || contrasena.length() < 6) {
            ra.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres.");
            ra.addFlashAttribute("token", token);
            return "redirect:/nueva-password?token=" + token;
        }

        if (!contrasena.equals(confirmarContrasena)) {
            ra.addFlashAttribute("error", "Las contraseñas no coinciden.");
            ra.addFlashAttribute("token", token);
            return "redirect:/nueva-password?token=" + token;
        }

        // Obtener usuario, cifrar la contraseña y actualizar
        Usuario usuario = resetToken.getUsuario();
        usuario.setContrasena(passwordEncoder.encode(contrasena));
        usuarioRepository.save(usuario);

        // Marcar token como usado
        resetToken.setUsado(true);
        tokenRepository.save(resetToken);

        ra.addFlashAttribute("success", "Contraseña restablecida correctamente. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }
}
