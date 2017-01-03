/*
 *    Copyright 2017, Optimizely
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.Matchers.argThat;
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
        expectMessage(level, msg, null);
    }

    public void expectMessage(Level level, String msg, Class<? extends Throwable> throwableClass) {
        expectedEvents.add(new ExpectedLogEvent(level, msg, throwableClass));
    }

    private void before() {
        initMocks(this);
        when(appender.getName()).thenReturn("MOCK");
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(appender);
    }

    private void verify() throws Throwable {
        for (final ExpectedLogEvent expectedEvent : expectedEvents) {
            Mockito.verify(appender).doAppend(argThat(new ArgumentMatcher<ILoggingEvent>() {
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

        private ExpectedLogEvent(Level level, String message, Class<? extends Throwable> throwableClass) {
            this.message = message;
            this.level = level;
            this.throwableClass = throwableClass;
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
