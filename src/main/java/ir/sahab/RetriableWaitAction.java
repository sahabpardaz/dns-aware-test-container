package ir.sahab;

/**
 * Default wait strategy of Test-containers is not a functional interface and can't be provided with a simple lambda.
 * To make code cleaner and make the work easier for end-user, this interface was created so users can provide a
 * wait strategy with a simple lambda and the lambda can throw exceptions.
 */
@FunctionalInterface
public interface RetriableWaitAction {
    void waitForAction() throws Exception;
}
