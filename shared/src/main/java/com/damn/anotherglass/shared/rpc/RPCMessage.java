package com.damn.anotherglass.shared.rpc;

import java.io.Serializable;

public class RPCMessage implements Serializable {
    public final String service;
    public final String type;
    public final Object payload;

    public <T extends Serializable> RPCMessage(String service, T obj) {
        this.service = service;
        this.type = null != obj ? obj.getClass().getName() : null;
        this.payload = obj;
    }

    public RPCMessage(String service, String typeName, Object payloadObj) {
        this.service = service;
        this.type = typeName;
        this.payload = payloadObj;
    }
}
