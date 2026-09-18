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
package com.teragrep.rlp_10;

import com.teragrep.net_01.channel.context.ConnectContextFactory;
import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.channel.socket.SocketFactory;
import com.teragrep.net_01.channel.socket.TLSFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_10.config.*;
import com.teragrep.rlp_10.report.MetricsReport;
import com.teragrep.rlp_10.report.PrometheusMetricsReport;
import com.teragrep.rlp_10.report.Slf4JMetricsReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Benchmark tests a relp endpoint
 */
public class Benchmark implements Callable<Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Benchmark.class);
    private final ExecutorService executorService;
    private final InitiatorConfig initiatorConfig;
    private final MetricsConfig metricsConfig;
    private final PrometheusConfig prometheusConfig;
    private final TimeoutConfig timeoutConfig;
    private final TransportConfig transportConfig;
    private final RecordStreamConfig recordStreamConfig;
    private final ReportConfig reportConfig;
    private final SocketAddressConfig socketAddressConfig;
    private final DelayConfig delayConfig;
    private final SyslogConfig syslogConfig;
    private final Map<EventLoop, List<Initiator>> eventLoops;
    private final List<MetricsReport> reports;
    private final List<Future<Long>> executorTasks;

    public Benchmark() {
        this(
                new InitiatorConfig(),
                new MetricsConfig(),
                new PrometheusConfig(),
                new TimeoutConfig(),
                new TransportConfig(),
                new RecordStreamConfig(),
                new ReportConfig(),
                new SocketAddressConfig(),
                new DelayConfig(),
                new SyslogConfig()
        );
    }

    public Benchmark(
            final InitiatorConfig initiatorConfig,
            final MetricsConfig metricsConfig,
            final PrometheusConfig prometheusConfig,
            final TimeoutConfig timeoutConfig,
            final TransportConfig transportConfig,
            final RecordStreamConfig recordStreamConfig,
            final ReportConfig reportConfig,
            final SocketAddressConfig socketAddressConfig,
            final DelayConfig delayConfig,
            final SyslogConfig syslogConfig
    ) {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.initiatorConfig = initiatorConfig;
        this.metricsConfig = metricsConfig;
        this.prometheusConfig = prometheusConfig;
        this.timeoutConfig = timeoutConfig;
        this.transportConfig = transportConfig;
        this.recordStreamConfig = recordStreamConfig;
        this.reportConfig = reportConfig;
        this.socketAddressConfig = socketAddressConfig;
        this.delayConfig = delayConfig;
        this.syslogConfig = syslogConfig;
        this.eventLoops = new HashMap<>(initiatorConfig.eventLoopCount());
        this.reports = new ArrayList<>();
        this.executorTasks = new ArrayList<>();
    }

    public Long call() {
        // todo configs

        final Metrics metrics = new Metrics(metricsConfig);

        // reports
        final PrometheusMetricsReport prometheusMetricsReport = new PrometheusMetricsReport(
                metrics.registry(),
                prometheusConfig
        );
        final Slf4JMetricsReport slf4JMetricsReport = new Slf4JMetricsReport(metrics.registry(), reportConfig);
        reports.add(prometheusMetricsReport);
        reports.add(slf4JMetricsReport);

        for (final MetricsReport report : reports) {
            report.start();
        }

        // recordStream is shared across all Initiators. Initiators ask for records until the recordstream is exhausted
        // todo use Hostname class from aer_02 or create new component for it
        final RecordStream recordStream = new RecordStreamImpl(
                "someOrigin",
                syslogConfig.hostname(),
                syslogConfig.appName(),
                recordStreamConfig.records()
        );

        // apply delay to recordStream if configured
        final RecordStream delayedStream;
        if (delayConfig.delay() > 0) {
            delayedStream = new RecordStreamDelay(delayConfig.delay(), recordStream);
        }
        else {
            delayedStream = recordStream;
        }

        final EventLoopFactory eventLoopFactory = new EventLoopFactory();
        final SocketFactory socketFactory = createSocketFactory();
        final int baseInitiators = initiatorConfig.initiatorCount() / initiatorConfig.eventLoopCount();
        final int remainder = initiatorConfig.initiatorCount() % initiatorConfig.eventLoopCount();
        final List<Initiator> initiators = new ArrayList<>();
        try {
            // create and start a thread for configured number of EventLoops and distribute configured number of Initiators among them equally
            for (int eventLoopCount = 0; eventLoopCount < initiatorConfig.eventLoopCount(); eventLoopCount++) {
                final EventLoop eventLoop = eventLoopFactory.create();
                executorService.submit(eventLoop);

                final ConnectContextFactory connectContextFactory = new ConnectContextFactory(
                        executorService,
                        socketFactory
                );

                // use remainder to determine if this EventLoop should get an additional Initiator or not in order to fit all Initiators within configured number of EventLoops
                final int initiatorsForEventLoop = baseInitiators + (eventLoopCount < remainder ? 1 : 0);
                final RelpClientFactory relpClientFactory = new RelpClientFactory(connectContextFactory, eventLoop);
                for (int initiatorCount = 0; initiatorCount < initiatorsForEventLoop; initiatorCount++) {
                    final Initiator initiator = new Initiator(
                            relpClientFactory,
                            delayedStream,
                            socketAddressConfig.hostname(),
                            socketAddressConfig.port(),
                            metrics,
                            timeoutConfig.openTimeout(),
                            timeoutConfig.payloadTimeout(),
                            initiatorConfig.retryTransmissionCount(),
                            initiatorConfig.retryConnectionCount()
                    );
                    executorTasks.add(executorService.submit(initiator));
                    initiators.add(initiator);
                }
                eventLoops.put(eventLoop, initiators);
            }

            // shutdown hook in case JVM is terminated
            final Thread shutdownHook = new Thread(this::stopBenchmark);
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            // block until each task is complete
            long totalRecords = 0;
            for (final Future<Long> task : executorTasks) {
                long taskRecords = task.get();
                totalRecords += taskRecords;
            }
            stopBenchmark();
            return totalRecords;
        }
        catch (final InterruptedException | ExecutionException | IOException e) {
            // unrecoverable exceptions
            throw new RuntimeException(e);
        }
    }

    private void stopBenchmark() {
        for (final Map.Entry<EventLoop, List<Initiator>> entry : eventLoops.entrySet()) {
            final List<Initiator> initiators = entry.getValue();
            final EventLoop eventLoop = entry.getKey();
            // tell each initiator to stop as soon as they can
            for (final Initiator initiator : initiators) {
                initiator.stop();
            }
            eventLoop.stop(); // do not close, stop it, it's automatic due to a "feature" https://github.com/teragrep/net_01/issues/30
        }

        for (final MetricsReport report : reports) {
            report.stop();
        }

        executorService.shutdown();
    }

    /**
     * A method that produces {@link SocketFactory}
     * 
     * @return
     */
    private SocketFactory createSocketFactory() {
        final SocketFactory rv;
        if (!transportConfig.tls()) {
            rv = new PlainFactory();
        }
        else {
            try {
                final SSLContext sslContext = SSLContext.getInstance(transportConfig.protocol());
                final KeyStore ks = KeyStore.getInstance("JKS");
                final KeyStore ts = KeyStore.getInstance("JKS");

                final File ksFile = transportConfig.keyStoreFile();
                final File tsFile = transportConfig.trustStoreFile();

                final FileInputStream ksFileIS = new FileInputStream(ksFile);
                final FileInputStream tsFileIS = new FileInputStream(tsFile);
                ts.load(tsFileIS, transportConfig.trustStorePassword().toCharArray());
                final TrustManagerFactory tmf = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ts);

                ks.load(ksFileIS, transportConfig.keyStorePassword().toCharArray());
                final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, transportConfig.keyStorePassword().toCharArray());
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                tsFileIS.close();
                ksFileIS.close();

                final Function<SSLContext, SSLEngine> sslEngineFunction = context -> {
                    final SSLEngine engine = context.createSSLEngine();
                    engine.setUseClientMode(true);
                    return engine;
                };
                rv = new TLSFactory(sslContext, sslEngineFunction);
            }
            catch (
                final KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException
                        | UnrecoverableKeyException | KeyManagementException e
            ) {
                // unrecoverable error
                throw new RuntimeException("Error while initializing TLS connection, check your configuration!", e);
            }
        }
        return rv;
    }
}
