package com.studia.aplikacja_trenerska;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import com.studia.aplikacja_trenerska.auth.entity.VerificationTokenType;
import com.studia.aplikacja_trenerska.auth.service.AccountMailService;
import com.studia.aplikacja_trenerska.config.AccountProperties;
import com.studia.aplikacja_trenerska.config.MailDeliveryProperties;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class MailDeliveryTests {
    private static final String FRONTEND = "https://trainer-demo.example.test";
    private static final String TOKEN = "private-token-fixture";
    private final Logger logger = (Logger) LoggerFactory.getLogger(AccountMailService.class);
    private ListAppender<ILoggingEvent> messages;

    @BeforeEach
    void captureDeliveryLogs() {
        messages = new ListAppender<>();
        messages.start();
        logger.addAppender(messages);
    }

    @AfterEach
    void detachLogCapture() {
        logger.detachAppender(messages);
        messages.stop();
    }

    @ParameterizedTest
    @CsvSource({"EMAIL_CONFIRMATION,confirm-email", "PASSWORD_RESET,reset-password", "ACCOUNT_DELETION,confirm-deletion"})
    void demoWritesEveryActionLinkWithoutSendingMailOrLoggingRecipient(VerificationTokenType type, String path) {
        JavaMailSender sender = mock(JavaMailSender.class);
        var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("mailSender", sender);
        var service = new AccountMailService(factory.getBeanProvider(JavaMailSender.class), account(null),
                new MailDeliveryProperties(MailDeliveryProperties.Mode.LOG));

        service.send("private-recipient@example.test", TOKEN, type, Duration.ofMinutes(30));

        verifyNoInteractions(sender);
        assertThat(messages.list).extracting(ILoggingEvent::getFormattedMessage)
                .contains("[DEMO MAIL] " + type + " " + FRONTEND + "/" + path + "?token=" + TOKEN)
                .noneMatch(message -> message.contains("private-recipient@example.test"));
    }

    @Test
    void demoDoesNotRequireAMailSenderBean() {
        var service = new AccountMailService(new DefaultListableBeanFactory().getBeanProvider(JavaMailSender.class),
                account(null), new MailDeliveryProperties(MailDeliveryProperties.Mode.LOG));
        assertThatCode(() -> service.send("unused@example.test", TOKEN, VerificationTokenType.EMAIL_CONFIRMATION,
                Duration.ofHours(24))).doesNotThrowAnyException();
    }

    @Test
    void smtpSendsTheLinkOnlyThroughMailAndDoesNotLeakItOnFailure() {
        JavaMailSender sender = mock(JavaMailSender.class);
        var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("mailSender", sender);
        var service = new AccountMailService(factory.getBeanProvider(JavaMailSender.class),
                account("sender@example.test"), new MailDeliveryProperties(MailDeliveryProperties.Mode.SMTP));
        service.send("recipient@example.test", TOKEN, VerificationTokenType.PASSWORD_RESET, Duration.ofMinutes(30));

        var mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(mail.capture());
        assertThat(mail.getValue().getText()).contains(FRONTEND + "/reset-password?token=" + TOKEN);
        assertThat(mail.getValue().getFrom()).isEqualTo("sender@example.test");

        doThrow(new MailSendException("Failure containing " + TOKEN)).when(sender).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> service.send("recipient@example.test", TOKEN,
                VerificationTokenType.PASSWORD_RESET, Duration.ofMinutes(30)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(messages.list).extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains(TOKEN) || message.contains("[DEMO MAIL]")
                        || message.contains("recipient@example.test"));
    }

    @Test
    void smtpStillRequiresASenderAddress() {
        assertThatIllegalArgumentException().isThrownBy(() -> new AccountMailService(
                new DefaultListableBeanFactory().getBeanProvider(JavaMailSender.class), account(""),
                new MailDeliveryProperties(MailDeliveryProperties.Mode.SMTP)))
                .withMessageContaining("mail-from");
    }

    @Test
    void smtpIsTheDefaultAndUnknownModesFailConfigurationBinding() {
        var runner = new ApplicationContextRunner().withUserConfiguration(MailConfiguration.class);
        runner.run(context -> assertThat(context.getBean(MailDeliveryProperties.class).mode())
                .isEqualTo(MailDeliveryProperties.Mode.SMTP));
        runner.withPropertyValues("app.mail.mode=log").run(context ->
                assertThat(context.getBean(MailDeliveryProperties.class).mode()).isEqualTo(MailDeliveryProperties.Mode.LOG));
        runner.withPropertyValues("app.mail.mode=unknown").run(context -> assertThat(context).hasFailed());
        for (String blankMode : new String[] {"", " "}) {
            runner.withPropertyValues("app.mail.mode=" + blankMode).run(context ->
                    assertThat(context.getBean(MailDeliveryProperties.class).mode()).isEqualTo(MailDeliveryProperties.Mode.SMTP));
        }
    }

    private AccountProperties account(String sender) {
        return new AccountProperties(FRONTEND, sender, Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofMinutes(30));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MailDeliveryProperties.class)
    static class MailConfiguration { }
}
