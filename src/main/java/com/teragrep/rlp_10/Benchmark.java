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

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
import com.teragrep.net_01.channel.context.ConnectContextFactory;
import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.channel.socket.SocketFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_10.config.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Benchmark {

    private final MetricRegistry metricRegistry;
    private final MetricsConfiguration metricsConfiguration;
    private final ExecutorService executorService;
    private final InitiatorConfig initiatorConfig;
    private final List<Initiator> initiators;
    private final ConsoleReporter reporter; // TODO: replace wtih http reporter

    public Benchmark() {
        this(new InitiatorConfig(), new MetricsConfiguration());
    }

    public Benchmark(InitiatorConfig initiatorConfig, MetricsConfiguration metricsConfiguration) {
        this.metricRegistry = new MetricRegistry();
        this.initiatorConfig = initiatorConfig;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.metricsConfiguration = metricsConfiguration;
        this.initiators = new ArrayList<>(initiatorConfig.count());
        this.reporter = ConsoleReporter
                .forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
    }

    public void startBenchmark() {
        // todo configs

        // todo: clean this mess up
        metricRegistry.counter("records");
        metricRegistry.counter("resends");
        metricRegistry.counter("connects");
        metricRegistry.counter("disconnects");
        metricRegistry.counter("retriedConnects");
        metricRegistry.timer("sendLatency", () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
        metricRegistry
                .timer("connectLatency", () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));

        reporter.start(1, TimeUnit.SECONDS);
        SocketAddressConfig socketAddressConfig = new SocketAddressConfig();

        // eventloop threads
        EventLoopFactory eventLoopFactory = new EventLoopFactory();
        try {
            EventLoop eventLoop = eventLoopFactory.create();
            executorService.submit(eventLoop);

            SocketFactory socketFactory = new PlainFactory();

            ConnectContextFactory connectContextFactory = new ConnectContextFactory(executorService, socketFactory);

            RelpClientFactory relpClientFactory = new RelpClientFactory(connectContextFactory, eventLoop);

            SyslogConfig syslogConfig = new SyslogConfig();

            // todo use Hostname class from aer_02 or create new component for it
            RecordStream recordStream = new RecordStreamImpl(
                    "someOrigin",
                    syslogConfig.hostname(),
                    syslogConfig.appName()
            );

            DelayConfig delayConfig = new DelayConfig();
            final RecordStream delayedStream;
            if (delayConfig.delay() > 0) {
                delayedStream = new RecordStreamDelay(delayConfig.delay(), recordStream);
            }
            else {
                delayedStream = recordStream;
            }

            for (int initiatorCount = 0; initiatorCount < initiatorConfig.count(); initiatorCount++) {
                Initiator initiator = new Initiator(
                        relpClientFactory,
                        delayedStream,
                        socketAddressConfig.hostname(),
                        socketAddressConfig.port(),
                        metricRegistry
                );
                executorService.submit(initiator);
                initiators.add(initiator);
            }

            // todo proper shutdown criteria. i.e. shutdown hook for ^C or count depleted

            //LockSupport.parkNanos(Long.MAX_VALUE);
        }
        catch (Exception ignored) {
            // todo handle properly
        }
    }

    public MetricRegistry metricRegistry() {
        return metricRegistry;
    }
}
