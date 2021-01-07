/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.udp;

import io.ballerina.runtime.api.Future;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 *  {@link UdpClient} creates the udp client and handles all the network operations.
 */
public class UdpClient {

    private final Channel channel;
    private final EventLoopGroup group;

    public UdpClient(InetSocketAddress localAddress, EventLoopGroup group) throws  InterruptedException {
        this.group = group;
        Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(SocketConstants.CONNECTIONLESS_CLIENT_HANDLER, new UdpClientHandler());
                    }
                });
        channel = clientBootstrap.bind(localAddress).sync().channel();
        channel.config().setAutoRead(false);
    }

    // needed for connection oriented client
    public void connect(SocketAddress remoteAddress, Future callback) throws  InterruptedException {
        channel.pipeline().replace(SocketConstants.CONNECTIONLESS_CLIENT_HANDLER,
                SocketConstants.CONNECT_CLIENT_HANDLER, new UdpConnectClientHandler());
        channel.connect(remoteAddress).sync().addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                callback.complete(SocketUtils.createSocketError("Can't connect to remote host"));
            }
        });
    }

    public void sendData(DatagramPacket datagram, Future callback) {
        channel.writeAndFlush(datagram).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                callback.complete(SocketUtils.createSocketError("Failed to send data"));
            } else {
                callback.complete(null);
            }
        });
    }

    public void receiveData(long readTimeout, Future callback)
            throws InterruptedException {

        channel.pipeline().addFirst(SocketConstants.READ_TIMEOUT_HANDLER, new IdleStateHandler(0, 0, readTimeout,
                TimeUnit.MILLISECONDS));

        if (channel.pipeline().get(SocketConstants.CONNECTIONLESS_CLIENT_HANDLER) != null) {
            UdpClientHandler handler = (UdpClientHandler) channel.pipeline().
                    get(SocketConstants.CONNECTIONLESS_CLIENT_HANDLER);
            handler.setCallback(callback);
        } else {
            UdpConnectClientHandler handler = (UdpConnectClientHandler) channel.pipeline().
                    get(SocketConstants.CONNECT_CLIENT_HANDLER);
            handler.setCallback(callback);
        }

        channel.read();
    }

    public void shutdown() throws InterruptedException {
        channel.close().sync();
    }
}
