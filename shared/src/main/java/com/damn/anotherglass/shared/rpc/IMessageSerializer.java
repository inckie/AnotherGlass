package com.damn.anotherglass.shared.rpc;

public interface IMessageSerializer {
    void writeMessage(RPCMessage message) throws Exception;
    RPCMessage readMessage() throws Exception;
}