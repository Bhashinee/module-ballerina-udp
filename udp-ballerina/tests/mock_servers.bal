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

import ballerina/io;

const int PORT1 = 9000;
const int PORT2 = 8080;
const int PORT3 = 9001;

listener Listener logServer = checkpanic new Listener(PORT1);
listener Listener echoServer = checkpanic new Listener(PORT2);
listener Listener botServer = checkpanic new Listener(PORT3);

service on logServer {

    remote function onBytes(readonly & byte[] data) returns Error? {
        io:println(getString(data));
    }

    remote function onError(readonly & Error err) {
        io:println(err.message());
    }
}

service on echoServer {

    remote function onBytes(readonly & byte[] data, Caller caller) returns Error? {
        io:println("Received by listener:", getString(data));
        // echo back
        Error? writeResult = caller->sendBytes(data);
    }

    remote function onError(readonly & Error err) {
        io:println(err);
    }
}

map<string> QuestionBank = {
    "hi": "hi there!",
    "who are you?": "I'm a ballerina bot"
};

service on botServer {

    remote function onDatagram(Datagram datagram, Caller caller) returns Error? {
        string|error? dataString = getString(datagram.data);
        io:println("Received data: ", dataString);
        if (dataString is string && QuestionBank.hasKey(dataString)) {
            string? response = QuestionBank[dataString];
            if (response is string) {
                Error? res = caller->sendDatagram(prepareDatagram(response, <string>caller.remoteHost,
                    <int>caller.remotePort));
            }
        }
        Error? res = caller->sendDatagram(prepareDatagram("Sorry,I Can’t help you with that",
        <string>caller.remoteHost, <int>caller.remotePort));
    }

    remote function onError(readonly & Error err) {
        io:println(err);
    }
}
