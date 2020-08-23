package com.damn.shared;

import java.io.Serializable;

public class RPCMessage implements Serializable {
    public final String service;
    public final String type;
    public final Object payload;

    public <T extends Serializable> RPCMessage(String service, T obj) {
        this.service = service;
        type = null != obj ? obj.getClass().getName() : null;
        payload = obj;
    }
}
