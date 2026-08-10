package com.raidsurvival.models;

public class MobSettings {

    private final String type;
    private int minWave = 1;
    private int minCount = 5;
    private int maxCount = 10;
    private int armorChance = 20;

    public MobSettings(String type) {
        this.type = type.toUpperCase();
    }

    public String getType() { return type; }
    public int getMinWave() { return minWave; }
    public void setMinWave(int minWave) { this.minWave = minWave; }
    public int getMinCount() { return minCount; }
    public void setMinCount(int minCount) { this.minCount = minCount; }
    public int getMaxCount() { return maxCount; }
    public void setMaxCount(int maxCount) { this.maxCount = maxCount; }
    public int getArmorChance() { return armorChance; }
    public void setArmorChance(int armorChance) { this.armorChance = armorChance; }
}
