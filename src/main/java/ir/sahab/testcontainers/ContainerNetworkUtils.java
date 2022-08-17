package ir.sahab.testcontainers;

import com.alibaba.dcm.DnsCacheManipulator;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

/**
 * Utilities to work with test-containers network and IPs.
 */
public class ContainerNetworkUtils {

    private static final Logger logger = LoggerFactory.getLogger(ContainerNetworkUtils.class);

    private ContainerNetworkUtils() {
        throw new AssertionError("This class is an utility class and must not be initialized");
    }

    public static String getContainerIp(InspectContainerResponse containerInfo) {
        return containerInfo.getNetworkSettings().getNetworks().values().stream()
                .map(ContainerNetwork::getIpAddress)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Couldn't retrieve container IP"));
    }

    /**
     * Simple utility method to extract the container IP.
     */
    public static String getContainerIp(Container<?> container) {
        return getContainerIp(container.getContainerInfo());
    }

    /**
     * Adds container IP with given DNS name to Java DNS.
     */
    public static void addContainerDns(String hostName, Container<?> container) {
        final String containerIp = getContainerIp(container);
        logger.info("Setting Hostname '{}' to container IP '{}' in JVM dns cache", hostName, containerIp);
        DnsCacheManipulator.setDnsCache(hostName, containerIp);
    }
}
