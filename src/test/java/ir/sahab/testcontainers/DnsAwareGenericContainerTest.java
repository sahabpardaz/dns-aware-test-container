package ir.sahab.testcontainers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DnsAwareGenericContainerTest {

    private static final String HBASE_HOSTNAME = "hbase";
    private static final Configuration hbaseConfig;
    private static final byte[] TEST_TABLE = Bytes.toBytes("test");
    private static final byte[] COLUMN_FAMILY = Bytes.toBytes("cf");
    private static final byte[] QUALIFIER = Bytes.toBytes("q");

    static {
        hbaseConfig = HBaseConfiguration.create();
        hbaseConfig.set(HConstants.ZOOKEEPER_QUORUM, HBASE_HOSTNAME);
        hbaseConfig.set(HConstants.ZOOKEEPER_CLIENT_PORT, "2181");
    }

    @Container
    DnsAwareGenericContainer container = new DnsAwareGenericContainer("harisekhon/hbase:1.4", HBASE_HOSTNAME)
            .withFixedExposedPorts(16010)
            .withExposedPorts(16030)
            .retryAndWaitFor("HBase coming up", () -> HBaseAdmin.checkHBaseAvailable(hbaseConfig));

    @Test
    void testHbaseContainer() throws IOException {
        try (Connection connection = ConnectionFactory.createConnection(hbaseConfig)) {
            TableName tableName = TableName.valueOf(TEST_TABLE);
            Admin hbaseAdmin = connection.getAdmin();
            assertFalse(hbaseAdmin.tableExists(tableName));

            hbaseAdmin.createTable(new HTableDescriptor(tableName).addFamily(new HColumnDescriptor(COLUMN_FAMILY)));
            assertTrue(hbaseAdmin.tableExists(tableName));

            try (Table table = connection.getTable(tableName)) {
                for (int i = 1; i < 20; i++) {
                    byte[] bytes = Bytes.toBytes(i);
                    table.put(new Put(bytes).addColumn(COLUMN_FAMILY, QUALIFIER, bytes));
                }

                ResultScanner scanner = table.getScanner(COLUMN_FAMILY);
                for (int i = 1; i < 20; i++) {
                    Result next = scanner.next();
                    int value = Bytes.toInt(next.getValue(COLUMN_FAMILY, QUALIFIER));
                    int rowKey = Bytes.toInt(next.getRow());
                    assertEquals(i, rowKey);
                    assertEquals(i, value);
                }
                scanner.close();
            }

            hbaseAdmin.disableTable(tableName);
            hbaseAdmin.deleteTable(tableName);
            assertFalse(hbaseAdmin.tableExists(tableName));

            HttpURLConnection urlConnection = (HttpURLConnection) new URL("http://localhost:16010").openConnection();
            assertDoesNotThrow(urlConnection::connect, "Can't connect to fixed master port with localhost");
            assertEquals(200, urlConnection.getResponseCode());

            urlConnection = (HttpURLConnection) new URL("http://localhost:" + container.getMappedPort(16030))
                    .openConnection();
            assertDoesNotThrow(urlConnection::connect, "Can't connect to mapped region-server port with localhost");
            assertEquals(200, urlConnection.getResponseCode());

            urlConnection = (HttpURLConnection) new URL("http://" + HBASE_HOSTNAME + ":16030").openConnection();
            assertDoesNotThrow(urlConnection::connect, "Can't connect to region-server port with DNS");
            assertEquals(200, urlConnection.getResponseCode());
        }

    }

}