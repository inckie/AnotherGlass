package com.damn.anotherglass.shared.gps;

import java.io.Serializable;

public class Location implements Serializable {
    public double latitude;
    public double longitude;
    public double altitude;
    public float speed;
    public float bearing;
    public float accuracy;
}
