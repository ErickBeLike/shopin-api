package com.app.shopin.services.mailtrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendPasswordResetCode(String userEmail, String code) {
        // Construye el cuerpo del correo de una forma más legible
        String messageBody = String.format(
                "Hola,\n\nHas solicitado restablecer tu contraseña.\n\n" +
                        "Usa el siguiente código de 6 dígitos para continuar: %s\n\n" +
                        "Este código expirará en 10 minutos.\n\n" +
                        "Si no solicitaste esto, puedes ignorar este correo de forma segura.",
                code
        );

        // Crea el objeto del correo
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(userEmail);
        mailMessage.setSubject("ShopIn - Código para Restablecer tu Contraseña");
        mailMessage.setText(messageBody);

        // Envía el correo
        javaMailSender.send(mailMessage);
    }
}