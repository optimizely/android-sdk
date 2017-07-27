package com.optimizely.ab.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.Appender;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * From http://techblog.kenshoo.com/2013/08/junit-rule-for-verifying-logback-logging.html
 */
public class LogbackVerifier implements TestRule {

    private List<ExpectedLogEvent> expectedEvents = new LinkedList<ExpectedLogEvent>();

    @Mock
    private Appender<ILoggingEvent> appender;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                    verify();
                } finally {
                    after();
                }
            }
        };
    }

    public void expectMessage(Level level) {
        expectMessage(level, "");
    }

    public void expectMessage(Level level, String msg) {
        expectMessage(level, msg, (Class<? extends Throwable>) null);
    }

    public void expectMessage(Level level, String msg, Class<? extends Throwable> throwableClass) {
        expectMessage(level, msg, null, times(1));
    }

    public void expectMessage(Level level, String msg, VerificationMode times) {
        expectMessage(level, msg, null, times);
    }

    public void expectMessage(Level level,
                              String msg,
                              Class<? extends Throwable> throwableClass,
                              VerificationMode times) {
        expectedEvents.add(new ExpectedLogEvent(level, msg, throwableClass, times));
    }

    private void before() {
        initMocks(this);
        when(appender.getName()).thenReturn("MOCK");
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(appender);
    }

    private void verify() throws Throwable {
        for (final ExpectedLogEvent expectedEvent : expectedEvents) {
            Mockito.verify(appender, expectedEvent.times).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {
                @Override
                public boolean matches(final Object argument) {
                    return expectedEvent.matches((ILoggingEvent) argument);
                }
            }));
        }
    }

    private void after() {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(appender);
    }

    private final static class ExpectedLogEvent {
        private final String message;
        private final Level level;
        private final Class<? extends Throwable> throwableClass;
        private final VerificationMode times;

        private ExpectedLogEvent(Level level,
                                 String message,
                                 Class<? extends Throwable> throwableClass,
                                 VerificationMode times) {
            this.message = message;
            this.level = level;
            this.throwableClass = throwableClass;
            this.times = times;
        }

        private boolean matches(ILoggingEvent actual) {
            boolean match = actual.getFormattedMessage().contains(message);
            match &= actual.getLevel().equals(level);
            match &= matchThrowables(actual);
            return match;
        }

        private boolean matchThrowables(ILoggingEvent actual) {
            IThrowableProxy eventProxy = actual.getThrowableProxy();
            return throwableClass == null || eventProxy != null && throwableClass.getName().equals(eventProxy.getClassName());
        }
    }
}
