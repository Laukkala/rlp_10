/*
 * Teragrep Reliable Event Logging Protocol (RELP) Library for Java
 * Copyright (C) 2021-2026 Suomen Kanuuna Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teragrep.rlp_10;

import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_10.config.SocketAddressConfig;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * These are a copy from rlp_03 test suite
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestServer.class);

    private EventLoop eventLoop;
    private Thread eventLoopThread;

    private ExecutorService executorService;

    private final List<byte[]> messageList = new LinkedList<>();

    @BeforeAll
    public void init() {
        SocketAddressConfig socketAddressConfig = new SocketAddressConfig();

        EventLoopFactory eventLoopFactory = new EventLoopFactory();
        Assertions.assertDoesNotThrow(() -> eventLoop = eventLoopFactory.create());

        eventLoopThread = new Thread(eventLoop);
        eventLoopThread.start();

        executorService = Executors.newSingleThreadExecutor();
        ServerFactory serverFactory = new ServerFactory(
                eventLoop,
                executorService,
                new PlainFactory(),
                new FrameDelegationClockFactory(() -> new DefaultFrameDelegate((frame) -> messageList.add(frame.relpFrame().payload().toBytes())))
        );
        Assertions.assertDoesNotThrow(() -> serverFactory.create(socketAddressConfig.port()));
    }

    @AfterAll
    public void cleanup() {
        eventLoop.stop();
        executorService.shutdown();
        Assertions.assertDoesNotThrow(() -> eventLoopThread.join());
    }

    @AfterEach
    public void clearMessageList() {
        // clear received list
        messageList.clear();
    }

    @Test
    public void testBenchmark() {
        Assertions.assertTrue(messageList.isEmpty());
        Benchmark benchmark = new Benchmark();
        Assertions.assertDoesNotThrow(()->new Thread(benchmark::startBenchmark).start());
        Assertions.assertDoesNotThrow(()->Thread.sleep(7000));
        Assertions.assertFalse(messageList.isEmpty());
        System.out.println(messageList.size());
        //TODO: see how we can measure latency
    }
}