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

import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * callback implementation.
 */
public class UdpCallback implements Callback {

    private static final Logger log = LoggerFactory.getLogger(UdpCallback.class);

    private UdpService udpService;
    private Channel channel;
    private DatagramPacket datagram;

    public UdpCallback(UdpService udpService, Channel channel, DatagramPacket datagram) {
        this.udpService = udpService;
        this.channel = channel;
        this.datagram = datagram;
    }

    public UdpCallback(UdpService udpService) {
        this.udpService = udpService;
    }

    public UdpCallback() {}

    @Override
    public void notifySuccess(Object object) {
        if (object instanceof BArray) {
            // call writeBytes if the service returns byte[]
            byte[] byteContent = ((BArray) object).getBytes();
            UdpListener.send(udpService, new DatagramPacket(Unpooled.wrappedBuffer(byteContent),
                            datagram.sender()), channel);
        } else if (object instanceof BMap) {
            // call sendDatagram if the service returns Datagram
            BMap<BString, Object> datagram = (BMap<BString, Object>) object;
            String host = datagram.getStringValue(StringUtils.fromString(Constants.DATAGRAM_REMOTE_HOST)).getValue();
            int port = datagram.getIntValue(StringUtils.fromString(Constants.DATAGRAM_REMOTE_PORT)).intValue();
            BArray data = datagram.getArrayValue(StringUtils.fromString(Constants.DATAGRAM_DATA));
            byte[] byteContent = data.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(Unpooled.wrappedBuffer(byteContent),
                    new InetSocketAddress(host, port));
            UdpListener.send(udpService, datagramPacket, channel);
        }
        log.debug("Method successfully dispatched.");
    }

    @Override
    public void notifyFailure(BError bError) {
        Dispatcher.invokeOnError(udpService, bError.getMessage());
        if (log.isDebugEnabled()) {
            log.debug(String.format("Method dispatch failed: %s", bError.getMessage()));
        }
    }
}
