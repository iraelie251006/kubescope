package tech.iraelie.kubescope.alerting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import tech.iraelie.kubescope.domain.alertRule.AlertCondition;
import tech.iraelie.kubescope.domain.alertRule.AlertRule;
import tech.iraelie.kubescope.domain.alertRule.MetricType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotifierTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private EmailNotifier notifier;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notifier, "fromEmail", "alerts@kubescope.io");
    }

    @Test
    void sendBuildsAndDispatchesAMessage() {
        AlertRule rule = new AlertRule();
        rule.setMetricType(MetricType.MONTHLY_COST);
        rule.setCondition(AlertCondition.GREATER_THAN);
        rule.setThresholdValue(new BigDecimal("100.00"));
        rule.setNotificationEmail("ops@example.com");

        notifier.send(rule, new BigDecimal("150.50"));

        ArgumentCaptor<SimpleMailMessage> cap = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(cap.capture());
        SimpleMailMessage msg = cap.getValue();

        assertThat(msg.getFrom()).isEqualTo("alerts@kubescope.io");
        assertThat(msg.getTo()).containsExactly("ops@example.com");
        assertThat(msg.getSubject()).contains("MONTHLY_COST").contains("100.00");
        assertThat(msg.getText())
                .contains("MONTHLY_COST")
                .contains("GREATER_THAN")
                .contains("100.00")
                .contains("150.5");
    }
}
