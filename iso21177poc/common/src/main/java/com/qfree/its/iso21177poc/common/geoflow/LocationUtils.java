package com.qfree.its.iso21177poc.common.geoflow;

import android.location.Location;

import com.qfree.its.location.Position;
import com.qfree.its.location.websocket.MyPosition;

import java.util.Date;

public class LocationUtils {

    public static Location qfreePositionToAndroidLocation(MyPosition position) {
        Location location = new Location(position.clazz);
        location.setTime(position.timestamp);
        location.setLongitude(position.lon);
        location.setLatitude(position.lat);
        location.setAltitude(position.height);
        location.setBearing((float) position.heading);
        location.setSpeed((float) position.velocity);
        return location;
    }

    public static QfreePosImpl androidLocationToQfreePos(Location l) throws Exception {
        QfreePosImpl qfreePos = new QfreePosImpl();
        qfreePos.setTimestamp(l.getTime());  // getTime is in milli seconds after 1970.
        qfreePos.setLatitude(l.getLatitude());
        qfreePos.setLongitude(l.getLongitude());
        qfreePos.setHeight(l.getAltitude());
        qfreePos.setHeading(l.getBearing());
        qfreePos.setVelocity(l.getSpeed());
        qfreePos.setSatelliteCount((l.getExtras() != null ?
                l.getExtras().getInt("satellites") : 0));
        qfreePos.setHdop((l.getAccuracy() < 5.0 ?
                1 : 5));
        qfreePos.setHorizontalProtectionLimit(l.getAccuracy() * 4);
        double[] arr = new double[9];
        arr[0] = l.getAccuracy() * l.getAccuracy();
        qfreePos.setPositionCovarianceMatrix(arr);
        return qfreePos;
    }
}
