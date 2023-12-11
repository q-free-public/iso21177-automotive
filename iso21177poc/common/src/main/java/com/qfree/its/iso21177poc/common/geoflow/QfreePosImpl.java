package com.qfree.its.iso21177poc.common.geoflow;

import com.qfree.its.location.GeoCalculations;
import com.qfree.its.location.Position;

import java.util.Date;
import java.util.Locale;

public class QfreePosImpl implements Position {

    private long timestamp;
    private double latitude;
    private double longitude;
    private double height;
    private double heading;
    private int satelliteCount;
    private double hdop;
    private double velocity;
    private double[] positionCovarianceMatrix;
    private double horizontalProtectionLimit;

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public double getHeading() {
        return heading;
    }

    @Override
    public int getFixType() {
        return 0;
    }

    @Override
    public void setFixType(int i) throws Exception {

    }

    @Override
    public int getSatelliteCount() {
        return satelliteCount;
    }

    @Override
    public double getHdop() {
        return hdop;
    }

    @Override
    public double getVelocityEast() {
        return 0;
    }

    @Override
    public double getVelocityNorth() {
        return 0;
    }

    @Override
    public double getVelocityUp() {
        return 0;
    }

    @Override
    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    @Override
    public double[] getVelocityCovarianceMatrix() {
        return new double[0];
    }

    @Override
    public double[] getPositionCovarianceMatrix() {
        return positionCovarianceMatrix;
    }

    @Override
    public double getHorizontalProtectionLimit() {
        return horizontalProtectionLimit;
    }

    @Override
    public String getFusionMode() {
        return null;
    }

    @Override
    public double calcDistance(Position position) {
        return GeoCalculations.calcDistance(this, position);
    }

    @Override
    public double calcBearing(Position position) {
        return 0;
    }

    @Override
    public Position getDestination(double v, double v1) {
        return null;
    }

    @Override
    public double getCrossTrackDistance(Position position) {
        return 0;
    }

    @Override
    public Position predict(Date date) {
        return null;
    }

    @Override
    public Position getIntersection(Position position) {
        return null;
    }

    @Override
    public void setVelocityEast(double v) throws Exception {

    }

    @Override
    public void setVelocityNorth(double v) throws Exception {

    }

    @Override
    public void setHeading(double v) throws Exception {
        this.heading = v;
    }

    @Override
    public void setHeight(double v) throws Exception {
        this.height = v;
    }

    @Override
    public void setTimestamp(long l) throws Exception {
        this.timestamp = l;
    }

    @Override
    public void setLatitude(double v) throws Exception {
        this.latitude = v;
    }

    @Override
    public void setLongitude(double v) throws Exception {
        this.longitude = v;
    }

    @Override
    public void setFusionMode(String s) throws Exception {

    }

    @Override
    public void setHorizontalProtectionLimit(double v) throws Exception {
        this.horizontalProtectionLimit = v;
    }

    @Override
    public void setVelocityCovarianceMatrix(double[] doubles) throws Exception {

    }

    @Override
    public void setPositionCovarianceMatrix(double[] doubles) throws Exception {
        this.positionCovarianceMatrix = doubles;
    }

    @Override
    public void setVelocityUp(double v) throws Exception {

    }

    @Override
    public void setHdop(double v) throws Exception {
        this.hdop = v;
    }

    @Override
    public void setSatelliteCount(int i) throws Exception {
        this.satelliteCount = i;
    }

    @Override
    public Position createPositionObject() {
        return null;
    }

    @Override
    public Position createPosition(double v, double v1) {
        return null;
    }

    @Override
    public Position clonePosition() {
        return null;
    }

    @Override
    public void setNonMutable() {

    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public void setUbloxFixType(int i) throws Exception {

    }

    @Override
    public int getUbloxFixType() {
        return 0;
    }

    @Override
    public String getUbloxFixTypeStr() {
        return null;
    }

    @Override
    public void setUbloxTimeToFirstFix(double v) throws Exception {

    }

    @Override
    public double getUbloxTimeToFirstFix() {
        return 0;
    }

    private static final String logLineGPSFormatStr =
            /*Longitude*/		"%.6f;" +
            /*Latitude*/		"%.6f;" +
            /*Altitude*/		"%.2f;" +
            /*Bearing*/			"%.2f;" +
            /*Speed*/			"%.2f;" +
            /*nSats*/			"%d;" +
            /*flag*/			"%d;" +
            /*hDop*/			"%.2f;" +
            /*integrity*/		"%.2f;";

    @Override
    public String toString() {
        return String.format(Locale.US, logLineGPSFormatStr,
                getLongitude(), getLatitude(),
                getHeight(), getHeading(), getVelocity(), getSatelliteCount(), getUbloxFixType(),
                getHdop(), ((positionCovarianceMatrix==null) ? 0.0 : Math.sqrt(getPositionCovarianceMatrix()[0])));
    }
}
