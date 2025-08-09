package com.damn.anotherglass.shared.rpc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

class ObjectMessageSerializer implements IMessageSerializer {

    private final ObjectInputStream ois;
    private final ObjectOutputStream oos;

    ObjectMessageSerializer(InputStream inputStream, OutputStream outputStream) throws IOException {
        ois = new ObjectInputStream(inputStream);
        oos = new ObjectOutputStream(outputStream);
    }

    @Override
    public void writeMessage(RPCMessage message) throws Exception {
        oos.writeObject(message);
        oos.flush();
    }

    @Override
    public RPCMessage readMessage() throws Exception {
        return (RPCMessage) ois.readObject();
    }
}
