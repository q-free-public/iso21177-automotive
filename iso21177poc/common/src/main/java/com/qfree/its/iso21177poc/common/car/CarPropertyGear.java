package com.qfree.its.iso21177poc.common.car;

public enum CarPropertyGear {
    NEUTRAL("N"),
    REVERSE("R"),
    PARK("P"),
    DRIVE("D"),
    UNDEFINED("U");

    private String shortName;

    CarPropertyGear(String s) {
        this.shortName = s;
    }

    public String getShortName() {
        return shortName;
    }
}
