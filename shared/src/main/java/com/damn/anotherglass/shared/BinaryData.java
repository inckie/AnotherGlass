package com.damn.anotherglass.shared;

import java.io.Serializable;

public class BinaryData implements Serializable {
    public byte[] bytes;
    public String mimeType;

    public BinaryData() {
    }

    public BinaryData(byte[] bytes, String mimeType) {
        this.bytes = bytes;
        this.mimeType = mimeType;
    }
}

