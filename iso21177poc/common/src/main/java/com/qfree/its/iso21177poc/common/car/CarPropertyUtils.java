package com.qfree.its.iso21177poc.common.car;

public class CarPropertyUtils {

    public static CarPropertyGear mapGear(int propvalue){
        switch (propvalue){
            case 1:
                return CarPropertyGear.NEUTRAL;
            case 2:
                return CarPropertyGear.REVERSE;
            case 4:
                return CarPropertyGear.PARK;
            case 8:
                return CarPropertyGear.DRIVE;
            default:
                return CarPropertyGear.UNDEFINED;
        }
    }

    public static CarPropertyIgnitionState mapIgnitionState(int propValue){
        switch (propValue){
            case 0:
            default:
                return CarPropertyIgnitionState.UNDEFINED;
            case 1:
                return CarPropertyIgnitionState.LOCK;
            case 2:
                return CarPropertyIgnitionState.OFF;
            case 3:
                return CarPropertyIgnitionState.ACC;
            case 4:
                return CarPropertyIgnitionState.ON;
            case 5:
                return CarPropertyIgnitionState.START;
        }
    }
}
