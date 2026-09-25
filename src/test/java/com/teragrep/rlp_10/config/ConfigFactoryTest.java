/*
 * Teragrep performance test application for RELP (rlp_10)
 * Copyright (C) 2026 Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */
package com.teragrep.rlp_10.config;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class ConfigFactoryTest {

    @Test
    void testDefaultConfig() {
        final Map<String, String> configMap = new HashMap<>();
        final ConfigFactory configFactory = new ConfigFactory(configMap);

        // you should be able to create every kind of configuration object.
        final InitiatorConfig initiatorConfig = Assertions.assertDoesNotThrow(() -> configFactory.initiatorConfig());
        final RecordStreamConfig recordStreamConfig = Assertions
                .assertDoesNotThrow(() -> configFactory.recordStreamConfig());
        final DelayConfig delayConfig = Assertions.assertDoesNotThrow(() -> configFactory.delayConfig());
        final MetricsConfig metricsConfig = Assertions.assertDoesNotThrow(() -> configFactory.metricsConfig());
        final PrometheusConfig prometheusConfig = Assertions.assertDoesNotThrow(() -> configFactory.prometheusConfig());
        final ReportConfig reportConfig = Assertions.assertDoesNotThrow(() -> configFactory.reportConfig());
        final SocketAddressConfig socketAddressConfig = Assertions
                .assertDoesNotThrow(() -> configFactory.socketAddressConfig());
        final SyslogConfig syslogConfig = Assertions.assertDoesNotThrow(() -> configFactory.syslogConfig());
        final TimeoutConfig timeoutConfig = Assertions.assertDoesNotThrow(() -> configFactory.timeoutConfig());
        final TransportConfig transportConfig = Assertions.assertDoesNotThrow(() -> configFactory.transportConfig());

        // every configuration object should contain valid values (eg. PrometheusConfig should not have a 'port' value that is out of range when using default configs, which would throw an Exception)
        Assertions.assertDoesNotThrow(() -> initiatorConfig.initiatorCount());
        Assertions.assertDoesNotThrow(() -> initiatorConfig.eventLoopCount());
        Assertions.assertDoesNotThrow(() -> initiatorConfig.retryConnectionCount());
        Assertions.assertDoesNotThrow(() -> initiatorConfig.retryTransmissionCount());
        Assertions.assertDoesNotThrow(() -> recordStreamConfig.records());
        Assertions.assertDoesNotThrow(() -> delayConfig.delay());
        Assertions.assertDoesNotThrow(() -> metricsConfig.window());
        Assertions.assertDoesNotThrow(() -> prometheusConfig.port());
        Assertions.assertDoesNotThrow(() -> reportConfig.interval());
        Assertions.assertDoesNotThrow(() -> reportConfig.durationTimeUnit());
        Assertions.assertDoesNotThrow(() -> reportConfig.rateTimeUnit());
        Assertions.assertDoesNotThrow(() -> socketAddressConfig.port());
        Assertions.assertDoesNotThrow(() -> socketAddressConfig.hostname());
        Assertions.assertDoesNotThrow(() -> syslogConfig.hostname());
        Assertions.assertDoesNotThrow(() -> syslogConfig.appName());
        Assertions.assertDoesNotThrow(() -> timeoutConfig.openTimeout());
        Assertions.assertDoesNotThrow(() -> timeoutConfig.payloadTimeout());
        Assertions.assertDoesNotThrow(() -> transportConfig.tls());
        Assertions.assertDoesNotThrow(() -> transportConfig.keyStoreFile());
        Assertions.assertDoesNotThrow(() -> transportConfig.keyStorePassword());
        Assertions.assertDoesNotThrow(() -> transportConfig.trustStoreFile());
        Assertions.assertDoesNotThrow(() -> transportConfig.trustStorePassword());
        Assertions.assertDoesNotThrow(() -> transportConfig.protocol());
    }

    @Test
    void testCustomConfig() {
        final Map<String, String> configMap = new HashMap<>();
        configMap.put("initiator.count", "10");
        configMap.put("recordstream.records", "100000");
        configMap.put("delay.duration", "PT1S");
        configMap.put("metrics.window", "2000");
        configMap.put("prometheus.port", "8000");
        configMap.put("report.interval", "60000");
        configMap.put("socket.hostname", "127.0.0.1");
        configMap.put("syslog.appname", "testApp");
        configMap.put("timeout.open", "2000");
        configMap.put("transport.tls", "true");
        final ConfigFactory configFactory = new ConfigFactory(configMap);

        // you should be able to create every kind of configuration object.
        final InitiatorConfig initiatorConfig = Assertions.assertDoesNotThrow(() -> configFactory.initiatorConfig());
        final RecordStreamConfig recordStreamConfig = Assertions
                .assertDoesNotThrow(() -> configFactory.recordStreamConfig());
        final DelayConfig delayConfig = Assertions.assertDoesNotThrow(() -> configFactory.delayConfig());
        final MetricsConfig metricsConfig = Assertions.assertDoesNotThrow(() -> configFactory.metricsConfig());
        final PrometheusConfig prometheusConfig = Assertions.assertDoesNotThrow(() -> configFactory.prometheusConfig());
        final ReportConfig reportConfig = Assertions.assertDoesNotThrow(() -> configFactory.reportConfig());
        final SocketAddressConfig socketAddressConfig = Assertions
                .assertDoesNotThrow(() -> configFactory.socketAddressConfig());
        final SyslogConfig syslogConfig = Assertions.assertDoesNotThrow(() -> configFactory.syslogConfig());
        final TimeoutConfig timeoutConfig = Assertions.assertDoesNotThrow(() -> configFactory.timeoutConfig());
        final TransportConfig transportConfig = Assertions.assertDoesNotThrow(() -> configFactory.transportConfig());

        // metrics with assigned values should have corresponding values in configuration objects. omitted values should remain as default
        Assertions.assertEquals(10, Assertions.assertDoesNotThrow(() -> initiatorConfig.initiatorCount()));
        Assertions.assertEquals(1, Assertions.assertDoesNotThrow(() -> initiatorConfig.eventLoopCount()));
        Assertions.assertEquals(3, initiatorConfig.retryConnectionCount());
        Assertions.assertEquals(3, initiatorConfig.retryTransmissionCount());
        Assertions.assertEquals(100000, recordStreamConfig.records());
        Assertions.assertEquals(1000000000, delayConfig.delay());
        Assertions.assertEquals(2000, metricsConfig.window());
        Assertions.assertEquals(8000, Assertions.assertDoesNotThrow(() -> prometheusConfig.port()));
        Assertions.assertEquals(60000, reportConfig.interval());
        Assertions.assertEquals(TimeUnit.MILLISECONDS, reportConfig.durationTimeUnit());
        Assertions.assertEquals(TimeUnit.SECONDS, reportConfig.rateTimeUnit());
        Assertions.assertEquals("127.0.0.1", socketAddressConfig.hostname());
        Assertions.assertEquals(1601, socketAddressConfig.port());
        Assertions.assertEquals("testApp", syslogConfig.appName());
        Assertions.assertEquals("localhost", syslogConfig.hostname());
        Assertions.assertEquals(2000, timeoutConfig.openTimeout());
        Assertions.assertEquals(5000, timeoutConfig.payloadTimeout());
        Assertions.assertEquals(true, transportConfig.tls());
        Assertions.assertEquals("changeit", transportConfig.keyStorePassword());
        Assertions.assertEquals("changeit", transportConfig.trustStorePassword());
        Assertions
                .assertEquals(Paths.get("/opt/teragrep/rlp_10/tls/keystore.jks").toAbsolutePath().toFile(), transportConfig.keyStoreFile());
        Assertions
                .assertEquals(Paths.get("/opt/teragrep/rlp_10/tls/truststore.jks").toAbsolutePath().toFile(), transportConfig.trustStoreFile());
        Assertions.assertEquals("TLSv1.3", transportConfig.protocol());
    }

    @Test
    public void testContract() {
        EqualsVerifier.forClass(ConfigFactory.class).verify();
    }

}
