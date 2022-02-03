package ir.sahab;

import com.alibaba.dcm.DnsCacheManipulator;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
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
     * fixed port mapping in test containers. However, some images need fixed ports or using dynamic port mapping
     * is so hard in some cases which may not be worthy. For this reason we make this method public here.
     */
    public void withFixedExposedPort(int hostPort, int containerPort) {
        super.addFixedExposedPort(hostPort, containerPort);
    }

    public String getContainerIp(InspectContainerResponse containerInfo) {
        return containerInfo.getNetworkSettings().getNetworks().values().stream()
                .map(ContainerNetwork::getIpAddress)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Container IP can't be detected."));
    }

    public String getContainerIp() {
        return getContainerIp(getContainerInfo());
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        final String ip = getContainerIp(containerInfo);
        logger.info("Setting Hostname '{}' to container IP '{}' in JVM dns cache", hostName, ip);
        DnsCacheManipulator.setDnsCache(hostName, ip);
    }

    @Override
    protected void containerIsStopping(InspectContainerResponse containerInfo) {
        logger.info("Removing Hostname '{}' from JVM dns cache", hostName);
        DnsCacheManipulator.removeDnsCache(hostName);
    }

}
