package com.damn.anotherglass.shared.rpc;

public interface IMessageSerializer {
    void writeMessage(RPCMessage message) throws Exception;
    RPCMessage readMessage() throws Exception;
    boolean isReady() throws Exception;
}