package tech.iraelie.kubescope.alerting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tech.iraelie.kubescope.domain.alertRule.AlertRule;

import java.math.BigDecimal;

@Service
public class EmailNotifier {

    private final JavaMailSender mailSender;

    @Value("${kubescope.alerts.from-email}")
    private String fromEmail;

    public EmailNotifier(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(AlertRule rule, BigDecimal value) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(rule.getNotificationEmail());
        msg.setSubject("[KubeScope] " + rule.getMetricType() + " above " + rule.getThresholdValue());
        msg.setText(buildBody(rule, value));
        mailSender.send(msg);
    }

    private String buildBody(AlertRule rule, BigDecimal value) {
        return """
                KubeScope alert triggered.

                Metric:    %s
                Condition: %s %s
                Current:   %s

                This alert will not re-fire for 1 hour.
                """.formatted(
                rule.getMetricType(),
                rule.getCondition(),
                rule.getThresholdValue().toPlainString(),
                value.toPlainString());
    }
}

