// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/log;
import ballerina/test;

@test:Config {dependsOn: [testContentReceive]}
function testConnectClientEcho() returns error? {
    ConnectClient socketClient = check new ("localhost", 48829);

    string msg = "Echo from connet client";
    check socketClient->writeBytes(msg.toBytes());
    log:printInfo("Data was sent to the remote host.");

    readonly & byte[] response = check socketClient->readBytes();
    test:assertEquals(string:fromBytes(response), msg, "Found unexpected output");

    return check socketClient->close();
}

@test:Config {dependsOn: [testConnectClientEcho]}
isolated function testInvalidLocalHostInConnectClient() {
    ConnectClient|Error? socketClient = new ("localhost", 48830, localHost = "invalid", timeout = 1.5);
    if socketClient is ConnectClient {
        test:assertFail("Provided invalid value for localHost this should return an Error");
    } else if socketClient is Error {
        log:printInfo("Error initializing UDP Client");
    }
}

@test:Config {dependsOn: [testInvalidLocalHostInConnectClient]}
isolated function testConnectClientReadTimeOut() returns error? {
    ConnectClient socketClient = check new ("localhost", 48830, localHost = "localhost", timeout = 1.5);

    var result = socketClient->readBytes();
    if result is byte[] {
        test:assertFail("No UDP service running on localhost:45830, no result should be returned");
    } else {
        test:assertEquals("Read timed out", result.message());
    }

    return check socketClient->close();
}
