# DNS aware test container
[![Tests](https://github.com/sahabpardaz/dns-aware-test-container/actions/workflows/ci.yml/badge.svg)](https://github.com/sahabpardaz/dns-aware-test-container/actions/workflows/ci.yml)
[![](https://jitpack.io/v/sahabpardaz/dns-aware-test-container.svg)](https://jitpack.io/#sahabpardaz/dns-aware-test-container)

[Testcontainers](https://www.testcontainers.org) is a useful library for using containerized applications within
tests. However, some applications and databases like Hadoop and HBase may need specific hostname resolutions
in order to work properly. This problem can sometimes be mitigated by setting the network mode of the containers
to *host* or  make them visible by DNS, but generally it is better to use proper DNS data for host resolution.

This library extends the GenericContainer from Testcontainers and adds the ability to manipulate the Java DNS cache
temporarily during the tests. It adds the given hostname to Java DNS cache which will point to tht IP of the container.
This eliminates the need to apply manual DNS settings on your physical machine and prevents the
pollution of your system's DNS data.

## Sample Usage
An example of using HBase inside a container based on `harisekhon/hbase:1.4` image using this library is shown below.
You can see a test based on this container in class [DnsAwareGenericContainerTest](https://github.com/sahabpardaz/dns-aware-test-container/blob/main/src/test/java/ir/sahab/DnsAwareGenericContainerTest.java)

```java
class HBaseTest {
    private static final String HBASE_HOSTNAME = "hbase";
    private static final Configuration hbaseConfig;
    static {
        hbaseConfig = HBaseConfiguration.create();
        hbaseConfig.set(HConstants.ZOOKEEPER_QUORUM, HBASE_HOSTNAME);
        hbaseConfig.set(HConstants.ZOOKEEPER_CLIENT_PORT, "2181");
    }

    @ClassRule
    var container = new DnsAwareGenericContainer("harisekhon/hbase:1.4", HBASE_HOSTNAME)
            .withFixedExposedPorts(16010)
            .withExposedPorts(16030)
            .retryAndWaitFor("HBase coming up", () -> HBaseAdmin.checkHBaseAvailable(hbaseConfig));
}
```

## Java version compatibility
This library is compatible with all java versions including 8,11,17. To use with Java 9+ you must pass these arguments 
to your JVM:
```shell
--add-opens java.base/java.net=ALL-UNNAMED
--add-opens java.base/sun.net=ALL-UNNAMED
```