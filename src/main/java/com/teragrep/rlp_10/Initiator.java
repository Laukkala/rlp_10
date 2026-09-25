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

import com.codahale.metrics.Timer;
import com.teragrep.rlp_03.client.RelpClient;
import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_03.client.RelpClientStub;
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_03.frame.RelpFrameFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class Initiator implements Callable<Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Initiator.class);
    private static final RelpFrameFactory relpFrameFactory = new RelpFrameFactory();
    private final RelpClientFactory relpClientFactory;
    private final RecordStream recordStream;
    private final Metrics metrics;
    private final String hostname;
    private final int port;
    private final long openTimeout;
    private final long payloadTimeout;
    private final int retryTransmissionCount;
    private final int retryConnectCount;

    private volatile boolean run = true;
    private final AtomicLong recordsSent;

    public Initiator(
            final RelpClientFactory relpClientFactory,
            final RecordStream recordStream,
            final String hostName,
            final int port,
            final Metrics metrics,
            final long connectTimeout,
            final long payloadTimeout,
            final int retryTransmissionCount,
            final int retryConnectCount
    ) {
        this.relpClientFactory = relpClientFactory;
        this.recordStream = recordStream;
        this.hostname = hostName;
        this.port = port;
        this.metrics = metrics;
        this.openTimeout = connectTimeout;
        this.payloadTimeout = payloadTimeout;
        this.retryTransmissionCount = retryTransmissionCount;
        this.retryConnectCount = retryConnectCount;
        this.recordsSent = new AtomicLong(); // todo move
    }

    @Override
    public Long call() {
        // producer threads
        try (final RelpClient relpClient = connect(retryConnectCount)) {
            if (!relpClient.isStub()) {
                // send syslog messageCount number of times
                while (run) {
                    final boolean sent = send(relpClient, retryTransmissionCount);
                    if (!sent) {
                        LOGGER.error("Failed to transmit data to server! Stopping...");
                        break;
                    }
                }
                // send close
                close(relpClient);
            }
            else {
                LOGGER.warn("RelpClient connection timeout! Stopping...");
            }
        }
        catch (final ExecutionException | InterruptedException exception) {
            LOGGER.error("Initiator encountered an unrecoverable error: ", exception);
        }
        return recordsSent.get();
    }

    private RelpClient connect(final int retryCount) throws InterruptedException, ExecutionException {
        final Timer.Context connectTimer = metrics.connectLatency().time();
        RelpClient rv = new RelpClientStub();
        for (int i = 0; i < retryCount; i++) {
            try {
                rv = connect();
                metrics.connects().inc();
                break;
            }
            catch (final TimeoutException timeoutException) {
                metrics.retriedConnects().inc();
                LOGGER.warn("Timeout reached while trying to establish RelpClient!");
            }
        }
        connectTimer.close();
        return rv;
    }

    private RelpClient connect() throws InterruptedException, ExecutionException, TimeoutException {
        final RelpClient relpClient = relpClientFactory
                .open(new InetSocketAddress(hostname, port))
                .get(openTimeout, TimeUnit.MILLISECONDS);

        final RelpFrame openFrame = relpFrameFactory.create("open", "a hallo yo client");
        final CompletableFuture<RelpFrame> open = relpClient.transmit(openFrame);
        open.get(openTimeout, TimeUnit.MILLISECONDS);
        return relpClient;
    }

    private boolean send(final RelpClient relpClient, final int retryCount)
            throws InterruptedException, ExecutionException {
        int retries = 0;
        final String payload = new String(recordStream.get(), StandardCharsets.UTF_8); // todo recordStream should return a stubable SyslogMessage, currently stubness is represented by empty bytearray
        boolean sent = send(relpClient, payload);
        while (!sent && retries < retryCount) {
            retries++;
            metrics.resends().inc();
            sent = send(relpClient, payload);
        }
        return sent;
    }

    private boolean send(final RelpClient relpClient, final String payload)
            throws InterruptedException, ExecutionException {
        try {
            // start transaction and transmit timers
            final Timer.Context transactionTimer = metrics.transactionLatency().time();
            final Timer.Context transmitTimer = metrics.transmitLatency().time();
            final Timer.Context receiveTimer;
            // stop transmit timer as soon as relpClient.transmit() finishes and start receiveTimer.
            if (!payload.isEmpty()) {
                final CompletableFuture<RelpFrame> syslog = relpClient
                        .transmit(relpFrameFactory.create("syslog", payload));

                // Transmission is complete as soon as relpClient.transmit() finishes.
                transmitTimer.close();
                receiveTimer = metrics.receiveLatency().time();
                syslog.get(payloadTimeout, TimeUnit.MILLISECONDS);
                recordsSent.incrementAndGet();
                // Whole transaction is complete as soon as Future received by transmit() is completed (or times out).
                receiveTimer.close();
                transactionTimer.close();
                metrics.records().inc();
                return true;
            }
            else {
                stop();
                return true;
            }
        }
        catch (final TimeoutException timeoutException) {
            LOGGER.warn("Send syslog attempt timeout after {} milliseconds!", payloadTimeout);
            return false;
        }
    }

    private void close(final RelpClient relpClient) throws InterruptedException, ExecutionException {
        final CompletableFuture<RelpFrame> close = relpClient.transmit(relpFrameFactory.create("close", ""));
        close.get();
        metrics.disconnects().inc();
    }

    public void stop() {
        run = false;
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
            final Initiator initiator = (Initiator) o;
            equals = port == initiator.port && openTimeout == initiator.openTimeout
                    && payloadTimeout == initiator.payloadTimeout
                    && retryTransmissionCount == initiator.retryTransmissionCount
                    && retryConnectCount == initiator.retryConnectCount && run == initiator.run && Objects
                            .equals(relpClientFactory, initiator.relpClientFactory)
                    && Objects.equals(recordStream, initiator.recordStream) && Objects.equals(metrics, initiator.metrics) && Objects.equals(hostname, initiator.hostname) && Objects.equals(recordsSent, initiator.recordsSent);
        }
        return equals;
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(
                        relpClientFactory, recordStream, metrics, hostname, port, openTimeout, payloadTimeout,
                        retryTransmissionCount, retryConnectCount, run, recordsSent
                );
    }
}
