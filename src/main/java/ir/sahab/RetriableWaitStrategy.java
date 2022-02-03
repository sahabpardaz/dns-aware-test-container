package ir.sahab;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

/**
 * Wait strategy that retries and waits for a {@link RetriableWaitAction}.
 */
public class RetriableWaitStrategy extends AbstractWaitStrategy {
    private static final Logger logger = LoggerFactory.getLogger(RetriableWaitStrategy.class);

    private final RetriableWaitAction waitStrategy;
    private final String name;

    private RetriableWaitStrategy(String name, RetriableWaitAction waitStrategy) {
        this.name = name;
        this.waitStrategy = waitStrategy;
    }

    /**
     * A factory method to create this wait strategy. It is created, so we can have a more readable and user-friendlier
     * interface to the outside world.
     */
    public static RetriableWaitStrategy retryAndWaitFor(String name, RetriableWaitAction waitStrategy) {
        return new RetriableWaitStrategy(name, waitStrategy);
    }

    @Override
    protected void waitUntilReady() {
        try {
            AtomicInteger attempts = new AtomicInteger(0);
            Unreliables.retryUntilSuccess((int) startupTimeout.toMillis(), TimeUnit.MILLISECONDS, () -> {
                    getRateLimiter().doWhenReady(() -> {
                        try {
                            waitStrategy.waitForAction();
                        } catch (Exception e) {
                            logger.info("Waiting for '{}'. attempts: {}", name, attempts.incrementAndGet());
                            logger.debug("Wait action of '{}' failed.", name, e);
                            throw new RuntimeException("Wait action failed.", e);
                        }
                    });
                return null; // Nothing should be returned here.
            });
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for '" + name + "'");
        }
    }
}
