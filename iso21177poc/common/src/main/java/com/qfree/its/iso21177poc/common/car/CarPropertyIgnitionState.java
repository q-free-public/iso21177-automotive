package com.qfree.its.iso21177poc.common.car;

public enum CarPropertyIgnitionState {
    UNDEFINED("undefined"),
    LOCK("lock"),
    OFF("off"),
    ACC("acc"),
    ON("on"),
    START("start");

    private String shortName;

    CarPropertyIgnitionState(String s) {
        this.shortName = s;
    }

    public String getShortName() {
        return shortName;
    }
}
