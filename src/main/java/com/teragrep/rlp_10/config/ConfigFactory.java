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

import com.teragrep.cnf_01.ConfigurationException;

import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ConfigFactory {

    private final Map<String, String> configValues;

    public ConfigFactory(final Map<String, String> configValues) {
        this.configValues = Collections.unmodifiableMap(configValues);
    }

    public DelayConfig delayConfig() throws ConfigurationException {
        final String configuredDuration = configValues.getOrDefault("delay.duration", "PT0S");
        try {
            final Duration delayDuration = Duration.parse(configuredDuration);
            return new DelayConfig(delayDuration);
        }
        catch (final DateTimeParseException dateTimeParseException) {
            throw new ConfigurationException(
                    "DelayConfig contains invalid configuration value!",
                    dateTimeParseException
            );
        }
    }

    public InitiatorConfig initiatorConfig() throws ConfigurationException {
        final String configuredInitiatorCount = configValues.getOrDefault("initiator.count", "1");
        final String configuredEventloopCount = configValues.getOrDefault("initiator.eventloopcount", "1");
        final String configuredRetryTransmissionCount = configValues
                .getOrDefault("initiator.retrytransmissioncount", "3");
        final String configuredRetryConnectionCount = configValues.getOrDefault("initiator.retryconnectioncount", "3");
        try {
            final int initiatorCount = Integer.parseInt(configuredInitiatorCount);
            final int eventloopCount = Integer.parseInt(configuredEventloopCount);
            final int retryTransmissionCount = Integer.parseInt(configuredRetryTransmissionCount);
            final int retryConnectionCount = Integer.parseInt(configuredRetryConnectionCount);
            return new InitiatorConfig(initiatorCount, retryTransmissionCount, retryConnectionCount, eventloopCount);
        }
        catch (final NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "InititatorConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public MetricsConfig metricsConfig() throws ConfigurationException {
        final String configuredWindow = configValues.getOrDefault("metrics.window", "10000");
        try {
            final int window = Integer.parseInt(configuredWindow);
            return new MetricsConfig(window);
        }
        catch (final NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "MetricsConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public PrometheusConfig prometheusConfig() throws ConfigurationException {
        final String configuredPort = configValues.getOrDefault("prometheus.port", "8080");
        try {
            final int port = Integer.parseInt(configuredPort);
            return new PrometheusConfig(port);
        }
        catch (final NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "PrometheusConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public RecordStreamConfig recordStreamConfig() throws ConfigurationException {
        final String configuredRecords = configValues
                .getOrDefault("recordstream.records", String.valueOf(Long.MAX_VALUE));
        try {
            final long records = Long.parseLong(configuredRecords);
            return new RecordStreamConfig(records);
        }
        catch (final NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "RecordStreamConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public ReportConfig reportConfig() throws ConfigurationException {
        final String configuredInterval = configValues.getOrDefault("report.interval", "1000");
        final String configuredRateTimeUnit = configValues.getOrDefault("report.ratetimeunit", "SECONDS");
        final String configuredDurationTimeUnit = configValues.getOrDefault("report.durationtimeunit", "MILLISECONDS");
        try {
            final long interval = Long.parseLong(configuredInterval);
            final TimeUnit rateTimeUnit = TimeUnit.valueOf(configuredRateTimeUnit);
            final TimeUnit durationTimeUnit = TimeUnit.valueOf(configuredDurationTimeUnit);
            return new ReportConfig(interval, rateTimeUnit, durationTimeUnit);
        }
        catch (final IllegalArgumentException illegalArgumentException) {
            throw new ConfigurationException(
                    "ReportConfig contains invalid configuration value!",
                    illegalArgumentException
            );
        }
    }

    public SocketAddressConfig socketAddressConfig() throws ConfigurationException {
        final String configuredHostname = configValues.getOrDefault("socket.hostname", "localhost");
        final String configuredPort = configValues.getOrDefault("socket.port", "1601");
        try {
            final int port = Integer.parseInt(configuredPort);
            return new SocketAddressConfig(configuredHostname, port);
        }
        catch (final NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "SocketAddressConfig contains invalid configuration value!",
                    numberFormatException
            );

        }
    }

    public SyslogConfig syslogConfig() {
        final String configuredHostname = configValues.getOrDefault("syslog.hostname", "localhost");
        final String configuredAppName = configValues.getOrDefault("syslog.appname", "appName");
        return new SyslogConfig(configuredHostname, configuredAppName);
    }

    public TimeoutConfig timeoutConfig() throws ConfigurationException {
        final String configuredOpenTimeout = configValues.getOrDefault("timeout.open", "1000");
        final String configuredPayloadTimeout = configValues.getOrDefault("timeout.payload", "5000");
        try {
            final long openTimeout = Long.parseLong(configuredOpenTimeout);
            final long payloadTimeout = Long.parseLong(configuredPayloadTimeout);
            return new TimeoutConfig(openTimeout, payloadTimeout);
        }
        catch (final NumberFormatException numberFormatException) {
            throw new ConfigurationException(
                    "TimeoutConfig contains invalid configuration value!",
                    numberFormatException
            );
        }
    }

    public TransportConfig transportConfig() throws ConfigurationException {
        final String configuredTls = configValues.getOrDefault("transport.tls", "false");
        final String configuredKeystorePassword = configValues.getOrDefault("transport.keystorepassword", "changeit");
        final String configuredTruststorePassword = configValues
                .getOrDefault("transport.truststorepassword", "changeit");
        final String protocol = configValues.getOrDefault("transport.protocol", "TLSv1.3");

        final String configuredKeystorePath = configValues
                .getOrDefault("transport.keystorepath", "/opt/teragrep/rlp_10/tls/keystore.jks");
        final String configuredTruststorePath = configValues
                .getOrDefault("transport.truststorepath", "/opt/teragrep/rlp_10/tls/truststore.jks");
        final Path baseDirectory = Path.of("/opt/teragrep/rlp_10");
        final Path keystorePath = Path.of(configuredKeystorePath).normalize();
        final Path truststorePath = Path.of(configuredTruststorePath).normalize();
        // Detect path traversal
        if (!keystorePath.startsWith(baseDirectory)) {
            throw new ConfigurationException(
                    "TransportConfig contains invalid keystore path! Keystore should be located within /opt/teragrep/rlp_10 directory!"
            );
        }
        if (!truststorePath.startsWith(baseDirectory)) {
            throw new ConfigurationException(
                    "TransportConfig contains invalid truststore path! Truststore should be located within /opt/teragrep/rlp_10 directory!"
            );
        }
        if (!"true".equalsIgnoreCase(configuredTls) && !"false".equalsIgnoreCase(configuredTls)) {
            throw new ConfigurationException("TransportConfig contains invalid TLS boolean!");
        }
        final boolean tls = Boolean.parseBoolean(configuredTls);
        return new TransportConfig(
                tls,
                keystorePath,
                truststorePath,
                configuredKeystorePassword,
                configuredTruststorePassword,
                protocol
        );
    }

    @Override
    public boolean equals(final Object o) {
        final boolean equals;
        if (this == o) {
            equals = true;
        }
        else if (o == null || getClass() != o.getClass()) {
            equals = false;
        }
        else {
            final ConfigFactory that = (ConfigFactory) o;
            equals = Objects.equals(configValues, that.configValues);
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(configValues);
    }
}
