package com.qfree.its.iso21177poc.common.app;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import androidx.core.content.res.ResourcesCompat;

import com.qfree.its.iso21177poc.common.R;
import com.qfree.nvdb.service.NvdbGeoflowZone;
import com.qfree.nvdb.service.NvdbPoint;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

public class OsmdroidUtils {
    private static final String TAG = OsmdroidUtils.class.getSimpleName();

    private static ArrayList<Polygon> mZones = new ArrayList<>();
    private static ArrayList<Marker> mMarkers = new ArrayList<>();
    private static ArrayList<Polyline> mRoutes = new ArrayList<>();

    public static void initMap(MapView map) {
        map.setTileSource(TileSourceFactory.MAPNIK);
        ColorMatrix negate = new ColorMatrix(new float[]{
                -1.0f, 0, 0, 0, 255,        //red
                0, -1.0f, 0, 0, 255,//green
                0, 0, -1.0f, 0, 255,//blue
                0, 0, 0, 1.0f, 0 //alpha
        });
        ColorMatrix gray = new ColorMatrix(new float[]{
                .33f, .33f, .33f, 0, 0,//red
                .33f, .33f, .33f, 0, 0,//green
                .33f, .33f, .33f, 0, 0,//blue
                0, 0, 0, 1, 0 //alpha
        });
        gray.postConcat(negate);
        map.getOverlayManager().getTilesOverlay().setColorFilter(new ColorMatrixColorFilter(gray.getArray()));
    }

    public static void setZoom(IMapController mapController, double zoomLevel) {
        mapController.setZoom(zoomLevel);
    }

    public static void setCenterPosition(IMapController mapController, GeoPoint geoPoint) {
        mapController.setCenter(geoPoint);
    }

    public static void drawGeoFlowZones(Context context, MapView map, ArrayList<NvdbGeoflowZone> zones) {
        clearZones(map);
        if (zones != null && !zones.isEmpty()) {
            for (NvdbGeoflowZone zone : zones) {
                ArrayList<GeoPoint> geoPoints = getGeoPoints(zone);
                Polygon polygon = new Polygon();
                polygon.setPoints(geoPoints);
                polygon.getFillPaint().setColor(context.getResources().getColor(R.color.zone_blue, null));
                polygon.getOutlinePaint().setColor(context.getResources().getColor(R.color.zone_blue, null));
                polygon.getOutlinePaint().setStrokeWidth(0);
                map.getOverlays().add(polygon);
                mZones.add(polygon);
            }
        }
    }

    private static ArrayList<GeoPoint> getGeoPoints(NvdbGeoflowZone zone) {
        ArrayList<GeoPoint> geoPoints = new ArrayList<>();
        for (NvdbPoint point : zone.polygon) {
            geoPoints.add(new GeoPoint(point.latitude, point.longitude));
        }
        return geoPoints;
    }

    public static void clearZones(MapView map) {
        for (Polygon polygon : mZones) {
            map.getOverlays().remove(polygon);
        }
    }

    public static void drawRoute(Context context, MapView map, IMapController mapController, ArrayList<GeoPoint> routePoints) {
        clearRoute(map);
        if (routePoints != null && !routePoints.isEmpty()) {
            Marker startMarker = new Marker(map);
            startMarker.setPosition((GeoPoint) routePoints.get(0));
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setIcon(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_place_24_green, null));
            startMarker.setInfoWindow(null);
            startMarker.setId("startMarker");
            map.getOverlays().add(startMarker);
            mMarkers.add(startMarker);

            Marker endMarker = new Marker(map);
            endMarker.setPosition((GeoPoint) routePoints.get(routePoints.size() - 1));
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            endMarker.setIcon(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_place_24_red, null));
            endMarker.setInfoWindow(null);
            endMarker.setId("endMarker");
            map.getOverlays().add(endMarker);
            mMarkers.add(endMarker);

            Polyline route = new Polyline();
            route.setPoints(routePoints);
            route.getOutlinePaint().setColor(context.getResources().getColor(R.color.route_red, null));
            route.getOutlinePaint().setStrokeWidth(5);
            route.setId("route");
            map.getOverlays().add(route);
            mRoutes.add(route);

            setCenterPosition(mapController, routePoints.get(routePoints.size() - 1));
        }
        map.postInvalidate();
    }

    public static void clearRoute(MapView map) {
        for (Marker marker : mMarkers) {
            map.getOverlays().remove(marker);
        }
        for (Polyline route : mRoutes) {
            map.getOverlays().remove(route);
        }
    }

    public static void clearAll(MapView map) {
        map.getOverlays().clear();
        map.invalidate();
    }
}
