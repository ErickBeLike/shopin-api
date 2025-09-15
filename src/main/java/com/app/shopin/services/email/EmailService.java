package com.app.shopin.services.email;

import com.app.shopin.modules.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    private String buildPersonalizedGreeting(User user) {
        return String.format("%s %s (%s)", user.getFirstName(), user.getLastName(), user.getUserName());
    }

    public void sendLoginNotification(User user) {
        // Obtenemos la fecha y hora actual para hacer el correo más informativo
        LocalDateTime now = LocalDateTime.now();
        // Le damos un formato amigable en español
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("eeee, d 'de' MMMM 'de' yyyy 'a las' HH:mm:ss", new Locale("es", "MX"));
        String formattedDateTime = now.format(formatter);
        String greeting = buildPersonalizedGreeting(user);

        String messageBody = String.format(
                "Hola, %s.\n\nDetectamos un nuevo inicio de sesión en tu cuenta de ShopIn.\n\n" +
                        "Fecha y Hora: %s (Hora del Centro de México)\n\n" +
                        "Si fuiste tú, puedes ignorar este mensaje.\n\n" +
                        "Si no reconoces esta actividad, por favor cambia tu contraseña de inmediato y revisa la seguridad de tu cuenta.",
                greeting, formattedDateTime
        );

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("ShopIn - Alerta de Inicio de Sesión");
        mailMessage.setText(messageBody);

        javaMailSender.send(mailMessage);
    }

    public void sendPasswordResetCode(User user, String code) {
        String greeting = buildPersonalizedGreeting(user);
        // Construye el cuerpo del correo de una forma más legible
        String messageBody = String.format(
                "Hola, %s.\n\nHas solicitado restablecer tu contraseña.\n\n" +
                        "Usa el siguiente código de 6 dígitos para continuar: %s\n\n" +
                        "Este código expirará en 10 minutos.\n\n" +
                        "Si no solicitaste esto, puedes ignorar este correo de forma segura.",
                greeting, code
        );

        // Crea el objeto del correo
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("ShopIn - Código para Restablecer tu Contraseña");
        mailMessage.setText(messageBody);

        // Envía el correo
        javaMailSender.send(mailMessage);
    }

    public void sendDeletionNoticeEmail(User user) {
        String greeting = buildPersonalizedGreeting(user);
        String messageBody = String.format(
                "Hola, %s.\n\nLamentamos que te vayas.\n\n" +
                        "Hemos recibido una solicitud para eliminar tu cuenta. Será eliminada permanentemente en 15 días.\n\n" +
                        "Si cambias de opinión, simplemente inicia sesión en tu cuenta antes de que pasen los 15 días y la solicitud de eliminación se cancelará automáticamente.\n\n" +
                        "Si no solicitaste esto, te recomendamos cambiar tu contraseña de inmediato.",
                greeting
        );

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("ShopIn - Tu cuenta será eliminada pronto");
        mailMessage.setText(messageBody);

        javaMailSender.send(mailMessage);
    }

    public void sendReactivationEmail(User user) {
        String greeting = buildPersonalizedGreeting(user);
        String messageBody = String.format(
                "¡Hola, %s!\n\n" +
                        "Bienvenido/a de nuevo.\n\n" +
                        "La solicitud para eliminar tu cuenta ha sido cancelada exitosamente. Tu cuenta está completamente activa y segura.\n\n" +
                        "Nos alegra tenerte de vuelta.",
                greeting
        );

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("ShopIn - Tu cuenta ha sido reactivada");
        mailMessage.setText(messageBody);

        javaMailSender.send(mailMessage);
    }

    // 2FA EMAIL CODE SEND SECTION
    public void sendEnableConfirmationCode(User user, String code) {
        String greeting = buildPersonalizedGreeting(user);

        String messageBody = String.format(
                "Hola, %s.\n\n" +
                        "Has solicitado activar la autenticación de dos factores por correo. Para completar el proceso, usa el siguiente código de verificación:\n\n" +
                        "Código: %s\n\n" +
                        "Este código expirará en 10 minutos.\n\n" +
                        "Si no solicitaste esto, cambia de inmediato tu contraseña.",
                greeting, code
        );

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("ShopIn - Activa la autenticación de dos factores");
        mailMessage.setText(messageBody);

        javaMailSender.send(mailMessage);
    }

    public void sendTwoFactorCode(User user, String code) {
        String greeting = buildPersonalizedGreeting(user);

        String messageBody = String.format(
                "Hola, %s.\n\n" +
                        "Estás intentando iniciar sesión. Para completar el proceso, usa el siguiente código de verificación:\n\n" +
                        "Código: %s\n\n" +
                        "Este código expirará en 5 minutos.\n\n" +
                        "Si no intentaste iniciar sesión, cambia de inmediato tu contraseña.",
                greeting, code
        );

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("ShopIn - Tu Código de Verificación");
        mailMessage.setText(messageBody);

        javaMailSender.send(mailMessage);
    }

    // 2FA EMAIL DISABLE CODE SEND SECTION
    public void sendDisableConfirmationCode(User user, String code) {
        String greeting = buildPersonalizedGreeting(user);

        String messageBody = String.format(
                "Hola, %s.\n\n" +
                        "Has solicitado desactivar un método de autenticación de dos factores. Para confirmar esta acción, usa el siguiente código:\n\n" +
                        "Código: %s\n\n" +
                        "Este código expirará en 10 minutos.\n\n" +
                        "Si no solicitaste esto, cambia de inmediato tu contraseña.",
                greeting, code
        );

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("ShopIn - Confirma la desactivación de 2FA");
        mailMessage.setText(messageBody);

        javaMailSender.send(mailMessage);
    }
}