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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.teragrep.rlp_03.client.RelpClient;
import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_03.frame.RelpFrameFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class Initiator implements Runnable {

    private static final RelpFrameFactory relpFrameFactory = new RelpFrameFactory();

    private final RelpClientFactory relpClientFactory;

    private final RecordStream recordStream;
    private final MetricRegistry metricRegistry;
    private final String hostname;
    private final int port;

    private volatile boolean run = true;

    //TODO: All initiators are currently in one eventLoop, allow for multiples.
    public Initiator(final RelpClientFactory relpClientFactory, final RecordStream recordStream, final MetricRegistry metricRegistry) {
        this(relpClientFactory, recordStream, "localhost", 1601, metricRegistry);
    }

    public Initiator(
            final RelpClientFactory relpClientFactory,
            final RecordStream recordStream,
            final String hostName,
            final int port,
            final MetricRegistry metricRegistry
    ) {
        this.relpClientFactory = relpClientFactory;
        this.recordStream = recordStream;
        this.hostname = hostName;
        this.port = port;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void run() {
        // producer threads

        try (
                final RelpClient relpClient = relpClientFactory.open(new InetSocketAddress(hostname, port)).get(1, TimeUnit.SECONDS);
        ) {
            // send open
            try(final Timer.Context timerContext = metricRegistry.timer("connectLatency").time()){
                final CompletableFuture<RelpFrame> open = relpClient
                        .transmit(relpFrameFactory.create("open", "a hallo yo client"));
                open.get();
                metricRegistry.counter("connects").inc();
            }

            while (run) {

                // todo use custom factory instead that takes bytes and not new String
                // send syslog

                try(final Timer.Context timerContext = metricRegistry.timer("sendLatency").time()) {
                    final CompletableFuture<RelpFrame> syslog = relpClient
                            .transmit(
                                    relpFrameFactory.create("syslog", new String(recordStream.get(), StandardCharsets.UTF_8))
                            );

                    // todo might want to configure multiple per batch and verify with .handleAsync();
                    metricRegistry.counter("records").inc();
                    syslog.get();
                }

            }

            // send close
            final CompletableFuture<RelpFrame> close = relpClient.transmit(relpFrameFactory.create("close", ""));
            close.get();
            metricRegistry.counter("disconnects").inc();

        }
        catch (final Exception e) {
            // todo log
            System.err.println(e.getMessage());
            run = false;
        }
    }

    public void stop() {
        run = false;
    }
}
