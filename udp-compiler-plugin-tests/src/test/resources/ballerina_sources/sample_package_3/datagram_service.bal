import ballerina/udp;

service on new udp:Listener(9000) {

    function onDatagram(readonly & udp:Datagram datagram) returns udp:Datagram {
        return datagram;
    }
}
