package com.teragrep.rlp_10;

import com.teragrep.rlp_03.client.RelpClient;
import com.teragrep.rlp_03.client.RelpClientFactory;
import com.teragrep.rlp_03.client.RelpClientImpl;
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
    private final String hostname;
    private final int port;

    private volatile boolean run = true;


    public Initiator(RelpClientFactory relpClientFactory, RecordStream recordStream) {
        this(relpClientFactory, recordStream, "localhost",1601);
    }

    public Initiator(RelpClientFactory relpClientFactory, RecordStream recordStream, String hostName, int port){
        this.relpClientFactory = relpClientFactory;
        this.recordStream = recordStream;
        this.hostname = hostName;
        this.port = port;
    }

    @Override
    public void run() {
        // producer threads

        try (
                RelpClient relpClient = relpClientFactory.open(new InetSocketAddress(hostname, port)).get(1, TimeUnit.SECONDS)
        ) {
            // send open
            CompletableFuture<RelpFrame> open = relpClient
                    .transmit(relpFrameFactory.create("open", "a hallo yo client"));

            open.get();

            while (run) {

                // todo use custom factory instead that takes bytes and not new String
                // send syslog
                CompletableFuture<RelpFrame> syslog = relpClient
                        .transmit(
                                relpFrameFactory.create("syslog", new String(recordStream.get(), StandardCharsets.UTF_8))
                        );

                // todo might want to configure multiple per batch and verify with .handleAsync();
                syslog.get();

            }

            // send close
            CompletableFuture<RelpFrame> close = relpClient.transmit(relpFrameFactory.create("close", ""));
            close.get();

        }
        catch (Exception e) {
            // todo log
            System.err.println(e.getMessage());
            run = false;
        }
    }

    public void stop() {
        run = false;
    }
}