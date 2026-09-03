package com.teragrep.rlp_10;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
import com.teragrep.rlp_03.client.RelpClient;
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_10.config.MetricsConfiguration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.codahale.metrics.MetricRegistry.name;

public class MeteredRelpClient implements RelpClient {

    final MetricRegistry metricRegistry;
    final RelpClient origin;

    public MeteredRelpClient(RelpClient origin, MetricRegistry metricRegistry){
        this.metricRegistry = metricRegistry;
        this.origin = origin;
    }

    @Override
    public CompletableFuture<RelpFrame> transmit(final RelpFrame relpFrame) {
        if(relpFrame.command().toString().equals("open")){
            try (final Timer.Context context = metricRegistry.timer(name(Benchmark.class, "connectLatency")).time()) {
                if(metricRegistry.counter(name(Benchmark.class, "connects")).getCount() == 0){
                    metricRegistry.counter(name(Benchmark.class, "connects")).inc();
                }
                else {
                    metricRegistry.counter(name(Benchmark.class, "retriedConnects")).inc();
                }
                //TODO: this blocks so its no good
                origin.transmit(relpFrame).get();
            }
            catch (ExecutionException | InterruptedException e){
                //TODO: error
            }
        }
        else {
            metricRegistry.counter(name(Benchmark.class, "records")).inc();
        }
        return origin.transmit(relpFrame);
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
