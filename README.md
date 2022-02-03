# DNS aware test container
[![Tests](https://github.com/MSDehghan/DnsAwareTestContainer/actions/workflows/maven.yml/badge.svg)](https://github.com/MSDehghan/DnsAwareTestContainer/actions/workflows/maven.yml)

[Testcontainers](https://www.testcontainers.org) is a very useful library which allows you to use containers in your
tests. However, some applications and databases like Hadoop and HBase works by using DNS data. To use the container of 
these applications you have to set the network of container to 'host' or change the DNS of your system to be
compatible to these containers.

This library extends the GenericContainer from Testcontainers and adds the ability to change the Java DNS temporarily
during the tests. With this library you are able to use applications that work by DNS easily without any manual
settings or pollution of your computer DNS data.

## Sample Usage
For example below we are using HBase database to create a new table in it. For more info please refer to tests.

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

# Java9+ Usage
This library is compatible to all java versions including 8,11,17. To use with java 9+ you must pass arguments 
to your jvm:
```shell
--add-opens java.base/java.net=ALL-UNNAMED
--add-opens java.base/sun.net=ALL-UNNAMED
```