package com.verdissia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateService templateService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.email.from:noreply@verdissia.com}")
    private String fromEmail;

    @Async
    public void sendSignatureEmail(String toEmail, String token, String clientName) {
        try {
            String signatureUrl = frontendUrl + "/signature?token=" + token;
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("VERDISSIA - Action requise : Finalisez votre signature");
            
            String htmlBody = buildHtmlEmailFromTemplate(clientName, signatureUrl, token);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Signature email sent successfully to {} with token {}", toEmail, token);
            
        } catch (Exception e) {
            log.error("Failed to send signature email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildHtmlEmailFromTemplate(String clientName, String signatureUrl, String token) {
        try {
            // Load template from file
            String templateContent = templateService.loadTemplate("/templates/email-signature.html");
            
            // Prepare template variables
            TemplateService.TemplateVariables variables = new TemplateService.TemplateVariables()
                .add("clientName", clientName)
                .add("signatureUrl", signatureUrl)
                .add("token", token)
                .add("tokenShort", token.length() > 8 ? token.substring(0, 8).toUpperCase() : token.toUpperCase());
            
            // Process template with variables
            return templateService.processTemplate(templateContent, variables);
            
        } catch (Exception e) {
            log.error("Failed to process email template", e);
            // Fallback to simple text email if template fails
            return buildFallbackEmail(clientName, signatureUrl, token);
        }
    }
    
    private String buildFallbackEmail(String clientName, String signatureUrl, String token) {
        return String.format("""
            <html>
            <body>
                <h2>VERDISSIA - Signature électronique</h2>
                <p>Bonjour %s,</p>
                <p>Veuillez cliquer sur le lien ci-dessous pour signer vos documents :</p>
                <p><a href="%s">Signer mes documents</a></p>
                <p>Référence : %s</p>
                <p>Ce lien est valide 24 heures.</p>
            </body>
            </html>
            """, clientName, signatureUrl, token);
    }
}
