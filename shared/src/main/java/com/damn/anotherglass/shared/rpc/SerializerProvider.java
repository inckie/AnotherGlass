package com.damn.anotherglass.shared.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerializerProvider {

    public enum SerializerType {
        JSON, OBJECT
    }

    private static final SerializerType currentSerializer = SerializerType.JSON;

    public static IMessageSerializer getSerializer(InputStream inputStream, OutputStream outputStream) throws IOException {
        return currentSerializer == SerializerType.JSON
                ? new JsonMessageSerializer(inputStream, outputStream)
                : new ObjectMessageSerializer(inputStream, outputStream);
    }
}
