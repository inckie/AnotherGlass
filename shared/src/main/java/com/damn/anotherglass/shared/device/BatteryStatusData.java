package com.damn.anotherglass.shared.device;

import java.io.Serializable;
import java.util.Objects;

// todo: use record (need to bump Java)
public class BatteryStatusData implements Serializable {
    public final int level;
    public final boolean isCharging;

    public BatteryStatusData(int level, boolean isCharging) {
        this.level = level;
        this.isCharging = isCharging;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatteryStatusData that = (BatteryStatusData) o;
        return level == that.level && isCharging == that.isCharging;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, isCharging);
    }

    @Override
    public String toString() {
        return "BatteryStatusData{" +
                "level=" + level +
                ", isCharging=" + isCharging +
                '}';
    }
}
