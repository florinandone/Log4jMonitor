import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Log4jMonitor {

    private static final String APPENDER_NAME = "MonitorAppender";
    private final Object lock;
    private Appender monitorAppender;
    private List<LogEvent> failedEvents;

    public Log4jMonitor() {
        lock = new Object();
        failedEvents = Collections.synchronizedList(new ArrayList<>());
        configureMonitorAppender();
    }

    private void configureMonitorAppender() {
        Configuration config = ((Logger) LogManager.getRootLogger()).getContext().getConfiguration();
        monitorAppender = new AbstractAppender(APPENDER_NAME, null, null) {
            @Override
            public void append(LogEvent event) {
                Level eventLevel = event.getLevel();
                Level monitorLevel = Level.getLevel(System.getProperty("monitor.log.level", "INFO"));
                if (eventLevel.isMoreSpecificThan(monitorLevel)) {
                    synchronized (lock) {
                        failedEvents.add(event);
                    }
                }
            }
        };
        monitorAppender.start();
        config.addAppender(monitorAppender);
        updateRootLogger(config);
        config.getRootLogger().addAppender(monitorAppender);
        LogManager.getContext(false).updateLoggers(config);
    }

    private void updateRootLogger(Configuration config) {
        LoggerConfig loggerConfig = config.getRootLogger();
        loggerConfig.removeAppender(APPENDER_NAME);
    }

    public void stopMonitoring() {
        Configuration config = ((Logger) LogManager.getRootLogger()).getContext().getConfiguration();
        LoggerConfig loggerConfig = config.getRootLogger();
        loggerConfig.removeAppender(APPENDER_NAME);
        config.getRootLogger().removeAppender(APPENDER_NAME);
        monitorAppender.stop();
        LogManager.getContext(false).updateLoggers(config);
    }

    public void failIfHigherLevelExists() throws LogEventException {
        synchronized (lock) {
            if (!failedEvents.isEmpty()) {
                throw new LogEventException(new ArrayList<>(failedEvents));
            }
        }
    }

    public static void main(String[] args) {
        Log4jMonitor monitor = new Log4jMonitor();

        // Testing
        Logger logger = LogManager.getLogger(Log4jMonitor.class);
        logger.error("Error message 1");
        logger.warn("Warning message 1");
        logger.info("Info message 1");

        try {
            monitor.failIfHigherLevelExists();
        } catch (LogEventException e) {
            List<LogEvent> failedEvents = e.getFailedEvents();
            System.out.println("Failed log events:");
            for (LogEvent event : failedEvents) {
                System.out.println("[" + event.getLevel() + "] " + event.getMessage().getFormattedMessage());
            }
        }

        monitor.stopMonitoring();
    }

    private static class LogEventException extends Exception {
        private final List<LogEvent> failedEvents;

        public LogEventException(List<LogEvent> failedEvents) {
            this.failedEvents = failedEvents;
        }

        public List<LogEvent> getFailedEvents() {
            return failedEvents;
        }
    }
}
