package com.qfree.its.iso21177poc.common.geoflow.thick_client;

import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qfree.geoflow.privatevault.CostTableService;
import com.qfree.geoflow.privatevault.NoTollContextException;
import com.qfree.geoflow.privatevault.NoZoneException;
import com.qfree.geoflow.toll.api.GeoFlowFactory;
import com.qfree.geoflow.toll.api.GeoFlowHoliday;
import com.qfree.geoflow.toll.api.GeoFlowZonePackage;
import com.qfree.its.iso21177poc.common.geoflow.GeoFlowSQLiteDb;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;
import com.qfree.its.location.Position;
import com.qfree.nvdb.service.NvdbDayTypes;
import com.qfree.nvdb.service.NvdbGeoflowZone;
import com.qfree.nvdb.service.NvdbRootObject;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

public class CostTableImpl implements CostTableService {
    private boolean rulesOk = false;
    private ArrayList<NvdbGeoflowZone> geoflowZones;
    private ArrayList<GeoFlowHoliday> geoFlowHolidays;
    private GeoFlowSQLiteDb mGeoFlowSQLiteDb;

    static LocalDate prevHolidayDate = null;
    static NvdbDayTypes prevHolidayType = null;

    public CostTableImpl(GeoFlowSQLiteDb geoFlowSQLiteDb) {
        this.mGeoFlowSQLiteDb = geoFlowSQLiteDb;
    }

    private NvdbGeoflowZone findZone(Position position) throws NoZoneException, NoTollContextException {
        if (geoflowZones == null) {
            throw new NoTollContextException("Zones not initiated");
        }
        for (NvdbGeoflowZone zone : geoflowZones) {
            if (zone.contains(position)){
                return zone;
            }
        }
        throw new NoZoneException("Not inside any zone");
    }

    @Override
    public double lookupCostPrKm(Position position, LocalDateTime localDateTime) throws NoZoneException, NoTollContextException {
        return findZone(position).getCostPrKm(getDayType(localDateTime), localDateTime.toLocalTime());
    }

    @Override
    public double lookupCostPrHour(Position position, LocalDateTime localDateTime) throws NoZoneException, NoTollContextException {
        return findZone(position).getCostPrHour(getDayType(localDateTime), localDateTime.toLocalTime());
    }

    @Override
    public String lookupZoneName(Position position) throws NoZoneException, NoTollContextException {
        return findZone(position).getZoneName();
    }

    @Override
    public String lookupCurrency(Position position) throws NoZoneException, NoTollContextException {
        String currency = findZone(position).getCurrency();
        if (currency == null || currency.isEmpty()) currency = "NOK";
        return currency;
    }

    @Override
    public void loadZonesFromSqLite(Connection connection) {
    }

    public void loadZonesFromSqLiteAndroid(){
        try {
            GeoFlowZonePackage zonePackage = this.mGeoFlowSQLiteDb.loadZonesFromSqLite();
            geoflowZones = getZonesFromPackage(zonePackage);
            sortZones();
            rulesOk = true;
        } catch (Exception e) {
            e.printStackTrace();
            rulesOk = false;
        }

    }

    @Override
    public void loadHolidaysFromSqLite(Connection connection) {

    }

    //TODO: Implement loadHolidays
    public void loadHolidaysFromSqLiteAndroid(SQLiteDatabase sqLiteDatabase) {


    }

    @Override
    public boolean hasRules() {
        return rulesOk;
    }

    @Override
    public Collection<NvdbGeoflowZone> getZoneList() {
        return geoflowZones;
    }

    @Override
    public Collection<GeoFlowHoliday> getHolidayList() {
        return geoFlowHolidays;
    }

    @Override
    public NvdbDayTypes getDayType(LocalDateTime localDateTime) {
        LocalDate date = localDateTime.toLocalDate();
        if (date.equals(prevHolidayDate)) {
            return prevHolidayType;
        }

        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            prevHolidayDate = date;
            prevHolidayType = NvdbDayTypes.holiday;
            return NvdbDayTypes.holiday;
        }

        if (geoFlowHolidays != null) {
            for (GeoFlowHoliday holiday : geoFlowHolidays) {
                if (date.equals(holiday.date)) {
                    prevHolidayDate = date;
                    prevHolidayType = NvdbDayTypes.holiday;
                    return NvdbDayTypes.holiday;
                }
            }
        }

        prevHolidayDate = date;
        prevHolidayType = NvdbDayTypes.weekday;
        return NvdbDayTypes.weekday;
    }

    @Override
    public void loadZonesFromServer(String serverUrl) {
    }

    public void loadZonesFromServerAndroid(String vehicleIdStr, String name, String serverUrl, Handler handler) throws Exception {
        HashMap<String, String> requestProperties = new HashMap<String, String>();
        //TODO: Fix requestProps
        requestProperties.put("X-Qfree-ItsStation", vehicleIdStr);
        requestProperties.put("X-Qfree-Hostname", name);
        if (geoflowZones != null) {
            LocalDateTime latestChange = NvdbRootObject.findLatestModification(geoflowZones);
            // If-Modified-Since: <day-name>, <day> <month> <year> <hour>:<minute>:<second> GMT
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE',' dd MMM yyyy HH:mm:ss 'GMT'").withLocale(Locale.US);
            String latestChangeStr = formatter.format(latestChange);
            requestProperties.put("If-Modified-Since", latestChangeStr);
            FileLogger.log("loadZonesFromServerAndroid: We have old zones " + latestChangeStr);
        } else {
            FileLogger.log("loadZonesFromServerAndroid: We don't have any zones");
        }
        TollZoneGetThread.doGet(serverUrl, requestProperties, handler);
    }

    @Override
    public void updateFromWeb(Connection connection, String s, byte[] bytes) throws Exception {
        FileLogger.log("updateFromWeb - does nothing!!!");

    }

    public void updateFromWebAndroid(String zones) throws Exception {
        Gson gson = GeoFlowFactory.createGson();
        GeoFlowZonePackage pkg = gson.fromJson(zones, GeoFlowZonePackage.class);
        this.mGeoFlowSQLiteDb.saveZonesToSqlLite(pkg);

        geoflowZones = getZonesFromPackage(pkg);
        sortZones();
        rulesOk = true;
    }

    private ArrayList<NvdbGeoflowZone> getZonesFromPackage(GeoFlowZonePackage pkg) throws Exception {
        Gson gson = GeoFlowFactory.createGson();
        Type listType = new TypeToken<ArrayList<NvdbGeoflowZone>>(){}.getType();
        ArrayList<NvdbGeoflowZone> zones = gson.fromJson(new String(pkg.body, "UTF-8"), listType);
        return zones;
    }

    private void sortZones() {
        geoflowZones.sort((NvdbGeoflowZone o1, NvdbGeoflowZone o2) -> o1.compareAreaTo(o2));

        // If we have "GeoFlow" zones, we remove the "GeoSum Trondheim" Zone
        int cntGeoFlow = 0;
        for (NvdbGeoflowZone z : geoflowZones) {
            if (z.description != null && z.description.contains("GeoFlow"))
                cntGeoFlow++;
        }
        if (cntGeoFlow > 0) {
            // Delete Trondheim/GeoSum
            geoflowZones.removeIf(z -> (z.description != null && z.description.contains("optIncludeLowEmZones") && z.name.startsWith("Trondheim")));
        }
    }

    public ArrayList<NvdbGeoflowZone> getGeoflowZones() {
        return geoflowZones;
    }
}
