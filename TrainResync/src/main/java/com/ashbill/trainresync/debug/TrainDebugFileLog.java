package com.ashbill.trainresync.debug;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TrainDebugFileLog {
    static final String PATH = "logs/trainresync-debug.log";
    private static final String NAME = "trainresync.debug.file";

    static Logger logger() {
        var context = (LoggerContext) LogManager.getContext(false);
        var config = context.getConfiguration();
        if (!config.getLoggers().containsKey(NAME)) {
            var appender = RollingFileAppender.newBuilder()
                    .setName(NAME).setConfiguration(config)
                    .withFileName(PATH).withFilePattern("logs/trainresync-debug-%i.log.gz")
                    .withPolicy(SizeBasedTriggeringPolicy.createPolicy("32 MB"))
                    .withStrategy(DefaultRolloverStrategy.newBuilder().withMax("3").withConfig(config).build())
                    .setLayout(PatternLayout.newBuilder().withCharset(StandardCharsets.UTF_8)
                            .withPattern("[%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}] %msg%n").build())
                    .build();
            if (appender == null) throw new IllegalStateException("Cannot open " + PATH);
            appender.start();
            config.addAppender(appender);
            var logger = new LoggerConfig(NAME, Level.INFO, false);
            logger.addAppender(appender, Level.INFO, null);
            config.addLogger(NAME, logger);
            context.updateLoggers();
        }
        return LoggerFactory.getLogger(NAME);
    }
}
