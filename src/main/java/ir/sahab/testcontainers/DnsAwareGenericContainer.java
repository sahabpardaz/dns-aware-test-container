package ir.sahab.testcontainers;

import com.alibaba.dcm.DnsCacheManipulator;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

/**
 * Specific type of container which sets the given hostname to container IP address automatically in Java DNS cache.
 */
public class DnsAwareGenericContainer extends GenericContainer<DnsAwareGenericContainer> {

    private static final Logger logger = LoggerFactory.getLogger(DnsAwareGenericContainer.class);

    private final String hostName;

    public DnsAwareGenericContainer(String image, String hostName) {
        super(image);

        this.hostName = hostName;
        withCreateContainerCmdModifier(cmd -> cmd.withHostName(hostName));
    }

    public DnsAwareGenericContainer retryAndWaitFor(String name, RetriableWaitAction waitStrategy) {
        return waitingFor(RetriableWaitStrategy.retryAndWaitFor(name, waitStrategy));
    }

    public DnsAwareGenericContainer withFixedExposedPorts(Integer... ports) {
        for (Integer port : ports) {
            withFixedExposedPort(port, port);
        }
        return this;
    }

    /**
     * {@link GenericContainer#addFixedExposedPort} is not public in GenericContainer to discourage the usage of
     * fixed port mappings in test containers. However, some images need fixed ports to work or using dynamic
     * port mapping is so hard in some cases which may not be worthy. For this reason we make this method public here.
     */
    public void withFixedExposedPort(int hostPort, int containerPort) {
        super.addFixedExposedPort(hostPort, containerPort);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        ContainerNetworkUtils.addContainerDns(hostName, this);
    }

    @Override
    protected void containerIsStopping(InspectContainerResponse containerInfo) {
        logger.info("Removing Hostname '{}' from JVM dns cache", hostName);
        DnsCacheManipulator.removeDnsCache(hostName);
    }

    public String getContainerIp() {
        return ContainerNetworkUtils.getContainerIp(this);
    }

}
