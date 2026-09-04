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
import com.teragrep.rlp_03.frame.RelpFrame;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MeteredRelpClient implements RelpClient {

    final MetricRegistry metricRegistry;
    final RelpClient origin;

    public MeteredRelpClient(RelpClient origin, MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.origin = origin;
    }

    @Override
    public CompletableFuture<RelpFrame> transmit(final RelpFrame relpFrame) {
        final CompletableFuture<RelpFrame> rv;
        if (relpFrame.command().toString().equals("open")) {
            try (final Timer.Context context = metricRegistry.timer("connectLatency").time()) {
                metricRegistry.counter("connects").inc(); // TODO: ConnectLatency already measures count so is connects necessary?
                rv = origin.transmit(relpFrame);
                rv.get(); // TODO: think of something else, this blocks
            }
            catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try (final Timer.Context context = metricRegistry.timer("sendLatency").time()) {
                metricRegistry.counter("records").inc();
                rv = origin.transmit(relpFrame);
            }
        }
        return rv;
    }

    @Override
    public void close() {
        origin.close();
    }

    @Override
    public boolean isStub() {
        return false;
    }
}
