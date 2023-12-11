package com.qfree.its.iso21177poc.common.app;

import com.qfree.its.iso21177poc.common.geoflow.QfreePosImpl;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

public class TripSummary {
    private double tripDistance;
    private double tripCost;
    private String currency;
    private ArrayList<GeoPoint> tripRoute;

    public TripSummary() {
        tripDistance = 0.0;
        tripCost = 0.0;
        tripRoute = new ArrayList<>();
    }

    public void addPosition(QfreePosImpl currentPosition) {
        GeoPoint pos = new GeoPoint(currentPosition.getLatitude(), currentPosition.getLongitude());
        if (!tripRoute.isEmpty()) {
            GeoPoint prevPos = tripRoute.get(tripRoute.size() - 1);
            double distance = prevPos.distanceToAsDouble(pos);
            if (distance > 10.0) {
                tripRoute.add(pos);
            }
            while (tripRoute.size() > 10000) {
                tripRoute.remove(0);
            }
        } else {
            tripRoute.add(pos);
        }
    }


    public double getTripDistance() {
        return tripDistance;
    }

    public void setTripDistance(double tripDistance) {
        this.tripDistance = tripDistance;
    }

    public double getTripCost() {
        return tripCost;
    }

    public void setTripCost(double tripCost) {
        this.tripCost = tripCost;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ArrayList<GeoPoint> getTripRoute() {
        return tripRoute;
    }

    public void setTripRoute(ArrayList<GeoPoint> tripRoute) {
        this.tripRoute = tripRoute;
    }

    public TripSummary minus(TripSummary tripZero) {
        if (tripZero == null) {
            return clone();
        } else {
            TripSummary ret = clone();
            ret.tripCost -= tripZero.tripCost;
            ret.tripDistance -= tripZero.tripDistance;
            return ret;
        }
    }

    public TripSummary clone() {
        TripSummary ret = new TripSummary();
        ret.tripDistance = this.tripDistance;
        ret.tripCost = this.tripCost;
        ret.currency = this.currency;
        ret.tripRoute = this.tripRoute;
        return ret;
    }

    public void clearRoute() {
        tripRoute = new ArrayList<>();
    }
}
