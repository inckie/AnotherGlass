package com.damn.anotherglass.shared.wifi;

import java.io.Serializable;

public class WiFiConfiguration implements Serializable {

    public String ssid;
    public String password;

    public WiFiConfiguration() {
    }

    public WiFiConfiguration(String ssid, String pass) {
        this.ssid = ssid;
        this.password = pass;
    }
}
