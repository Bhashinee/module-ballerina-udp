import ballerina/udp as u;

service on  new u:Listener(8000) {

   remote function onBytes(readonly & byte[] data) returns byte[] | u:Error? {

   }

   remote function onError(int err) {

   }
}
