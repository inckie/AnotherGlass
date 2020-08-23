package com.damn.anotherglass.glass.host.utility;

import java.io.Closeable;
import java.io.IOException;

public class Closeables {

    public static void close(Closeable closable) {
        if(null != closable) {
            try {
                closable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
