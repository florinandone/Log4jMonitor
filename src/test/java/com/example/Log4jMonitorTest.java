import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Log4jMonitorTest {

    private Log4jMonitor monitor;
    private Logger logger;

    @BeforeEach
    public void setUp() {
        monitor = new Log4jMonitor();
        logger = (Logger) LogManager.getLogger(Log4jMonitor.class);
    }

    @AfterEach
    public void tearDown() {
        monitor.stopMonitoring();
    }

    @Test
    public void testFailIfHigherLevelExists_NoFailedEvents() {
        logger.info("Info message");

        assertDoesNotThrow(() -> monitor.failIfHigherLevelExists());
    }

    @Test
    public void testFailIfHigherLevelExists_FailedEventsExist() {
        logger.error("Error message 1");
        logger.warn("Warning message 1");
        logger.info("Info message 1");

        Log4jMonitor.LogEventException exception = assertThrows(Log4jMonitor.LogEventException.class, () -> monitor.failIfHigherLevelExists());
        assertEquals(2, exception.getFailedEvents().size());
    }
}
