package com.raiderworld.models;

import java.util.ArrayList;
import java.util.List;

public class MobSettings {

    private final String type;
    private int minWave = 1;
    private int minCount = 5;
    private int maxCount = 15;
    private int armorChance = 20;
    private int weaponChance = 10;
    private int incompleteArmorChance = 25;
    private final List<String> effects = new ArrayList<>(); // e.g. "SPEED:1:10" (effect:amplifier:chance)

    public MobSettings(String type) {
        this.type = type;
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
    public int getWeaponChance() { return weaponChance; }
    public void setWeaponChance(int weaponChance) { this.weaponChance = weaponChance; }
    public int getIncompleteArmorChance() { return incompleteArmorChance; }
    public void setIncompleteArmorChance(int incompleteArmorChance) { this.incompleteArmorChance = incompleteArmorChance; }
    public List<String> getEffects() { return effects; }
}
