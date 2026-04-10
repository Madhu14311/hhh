package com.blisssierra.hrms.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send a beautifully designed HTML OTP email to the employee during signup.
     */
    public void sendOtpEmail(String toEmail, String employeeName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // true = multipart, true = html
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Bliss Sierra Software Solutions");
            helper.setTo(toEmail);
            helper.setSubject("Your OTP for Registration — Bliss Sierra");
            helper.setText(buildHtmlBody(employeeName, otp), true); // true = isHtml

            mailSender.send(message);
            log.info("✅ OTP HTML email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error sending OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML EMAIL TEMPLATE
    // ─────────────────────────────────────────────────────────────────────────

    private String buildHtmlBody(String name, String otp) {
        // Split OTP into individual characters for the big digit display
        String[] digits = otp.split("");
        StringBuilder digitBoxes = new StringBuilder();
        for (String digit : digits) {
            digitBoxes.append(
                    "<td style=\"padding: 0 6px;\">" +
                            "<div style=\"" +
                            "width: 48px;" +
                            "height: 60px;" +
                            "background: #fff;" +
                            "border: 2px solid #e2e8f0;" +
                            "border-radius: 10px;" +
                            "display: inline-block;" +
                            "line-height: 60px;" +
                            "text-align: center;" +
                            "font-size: 28px;" +
                            "font-weight: 800;" +
                            "color: #E8500A;" +
                            "font-family: 'Courier New', monospace;" +
                            "box-shadow: 0 2px 8px rgba(232,80,10,0.10);" +
                            "\">" + digit + "</div>" +
                            "</td>");
        }

        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\" />" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />" +
                "  <title>OTP Verification — Bliss Sierra</title>" +
                "</head>" +
                "<body style=\"margin:0;padding:0;background:#f0f4f8;font-family:'Segoe UI',Arial,sans-serif;\">" +

                "<!-- Outer wrapper -->" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f0f4f8;padding:40px 0;\">"
                +
                "<tr><td align=\"center\">" +

                "<!-- Card -->" +
                "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" " +
                "style=\"max-width:600px;width:100%;background:#ffffff;border-radius:16px;" +
                "overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.10);\">" +

                "<!-- ══ HEADER BANNER ══ -->" +
                "<tr>" +
                "  <td style=\"background:linear-gradient(135deg,#E8500A 0%,#c94008 100%);padding:36px 40px 32px;text-align:center;\">"
                +
                "    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "      <tr>" +
                "        <td align=\"center\" style=\"padding-bottom:10px;\">" +
                "          <!-- Logo icon -->" +
                "          <div style=\"display:inline-block;background:rgba(255,255,255,0.15);border-radius:50%;width:64px;height:64px;line-height:64px;font-size:30px;margin-bottom:12px;\">🏢</div>"
                +
                "        </td>" +
                "      </tr>" +
                "      <tr>" +
                "        <td align=\"center\">" +
                "          <h1 style=\"margin:0;color:#ffffff;font-size:26px;font-weight:900;letter-spacing:2px;text-transform:uppercase;\">Bliss Sierra</h1>"
                +
                "          <p style=\"margin:4px 0 0;color:rgba(255,255,255,0.85);font-size:13px;letter-spacing:3px;text-transform:uppercase;font-weight:500;\">Software Solutions</p>"
                +
                "        </td>" +
                "      </tr>" +
                "    </table>" +
                "  </td>" +
                "</tr>" +

                "<!-- ══ GREETING ══ -->" +
                "<tr>" +
                "  <td style=\"padding:40px 44px 10px;\">" +
                "    <h2 style=\"margin:0 0 8px;font-size:22px;color:#1a202c;font-weight:700;\">Hello, "
                + escapeHtml(name) + "! 👋</h2>" +
                "    <p style=\"margin:0;font-size:15px;color:#4a5568;line-height:1.7;\">" +
                "      Welcome to <strong style=\"color:#E8500A;\">Bliss Sierra Software Solutions HRMS</strong>.<br/>"
                +
                "      To complete your employee account registration, please use the One-Time Password (OTP) below." +
                "    </p>" +
                "  </td>" +
                "</tr>" +

                "<!-- ══ OTP BOX ══ -->" +
                "<tr>" +
                "  <td style=\"padding:30px 44px;\">" +
                "    <div style=\"background:linear-gradient(135deg,#fff5f0 0%,#fff0eb 100%);border:1.5px solid #fbd5c5;border-radius:14px;padding:28px 20px;text-align:center;\">"
                +
                "      <p style=\"margin:0 0 16px;font-size:12px;font-weight:700;letter-spacing:3px;text-transform:uppercase;color:#9b4010;\">Your One-Time Password</p>"
                +
                "      <!-- Digit boxes -->" +
                "      <table cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 auto 16px;\">" +
                "        <tr>" + digitBoxes + "</tr>" +
                "      </table>" +
                "      <p style=\"margin:0;font-size:13px;color:#a0aec0;\">" +
                "        ⏱&nbsp; Valid for <strong>10 minutes</strong> only" +
                "      </p>" +
                "    </div>" +
                "  </td>" +
                "</tr>" +

                "<!-- ══ SECURITY NOTE ══ -->" +
                "<tr>" +
                "  <td style=\"padding:0 44px 32px;\">" +
                "    <div style=\"background:#f7fafc;border-left:4px solid #E8500A;border-radius:0 8px 8px 0;padding:14px 18px;\">"
                +
                "      <p style=\"margin:0;font-size:13px;color:#4a5568;line-height:1.7;\">" +
                "        🔒 <strong>Security Notice:</strong> Never share this OTP with anyone. " +
                "        Bliss Sierra staff will <em>never</em> ask for your OTP via phone or email." +
                "      </p>" +
                "    </div>" +
                "  </td>" +
                "</tr>" +

                "<!-- ══ IGNORE NOTICE ══ -->" +
                "<tr>" +
                "  <td style=\"padding:0 44px 36px;\">" +
                "    <p style=\"margin:0;font-size:13px;color:#718096;line-height:1.7;\">" +
                "      If you did not initiate this registration request, please ignore this email " +
                "      or contact our HR team immediately at " +
                "      <a href=\"mailto:hr@blisssierra.com\" style=\"color:#E8500A;text-decoration:none;font-weight:600;\">hr@blisssierra.com</a>."
                +
                "    </p>" +
                "  </td>" +
                "</tr>" +

                "<!-- ══ DIVIDER ══ -->" +
                "<tr>" +
                "  <td style=\"padding:0 44px;\"><hr style=\"border:none;border-top:1px solid #e2e8f0;margin:0;\"/></td>"
                +
                "</tr>" +

                "<!-- ══ FOOTER ══ -->" +
                "<tr>" +
                "  <td style=\"padding:24px 44px 28px;text-align:center;\">" +
                "    <p style=\"margin:0 0 4px;font-size:13px;font-weight:700;color:#2d3748;\">Bliss Sierra Software Solutions</p>"
                +
                "    <p style=\"margin:0;font-size:12px;color:#a0aec0;\">© 2026 Bliss Sierra Software Solutions · All rights reserved</p>"
                +
                "    <p style=\"margin:8px 0 0;font-size:11px;color:#cbd5e0;\">This is an automated email — please do not reply directly.</p>"
                +
                "  </td>" +
                "</tr>" +

                "</table>" +
                "<!-- End card -->" +

                "</td></tr>" +
                "</table>" +
                "<!-- End outer wrapper -->" +

                "</body>" +
                "</html>";
    }

    /**
     * Escape HTML special characters to prevent injection in the employee name
     * field.
     */
    private String escapeHtml(String input) {
        if (input == null)
            return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}