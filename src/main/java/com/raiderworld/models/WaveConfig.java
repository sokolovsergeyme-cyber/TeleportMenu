package com.raiderworld.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Optional per-wave overrides. Currently most configuration is driven by MobSettings.minWave + counts.
 */
public class WaveConfig {

    private final int waveNumber;
    private final Map<String, int[]> mobCounts = new HashMap<>(); // type -> [min, max]

    public WaveConfig(int waveNumber) {
        this.waveNumber = waveNumber;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public void setMobCount(String type, int min, int max) {
        mobCounts.put(type.toUpperCase(), new int[]{min, max});
    }

    public Map<String, int[]> getMobCounts() {
        return mobCounts;
    }
}
