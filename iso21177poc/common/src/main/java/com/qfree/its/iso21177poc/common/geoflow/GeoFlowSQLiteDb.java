package com.qfree.its.iso21177poc.common.geoflow;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.qfree.geoflow.toll.api.GeoFlowInvoiceItem;
import com.qfree.geoflow.toll.api.GeoFlowInvoiceSummary;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordItemInvoiceItem;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordSecured;
import com.qfree.geoflow.toll.api.GeoFlowUserRecord;
import com.qfree.geoflow.toll.api.GeoFlowUtils;
import com.qfree.geoflow.toll.api.GeoFlowVehicleRecord;
import com.qfree.geoflow.toll.api.GeoFlowZonePackage;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class GeoFlowSQLiteDb extends SQLiteOpenHelper {
    public static final int DB_VERSION = 2;
    public static final String DB_FILENAME = "geoflow-database.db";
    private static final String TAG = GeoFlowSQLiteDb.class.getSimpleName();
    private SQLiteDatabase mSqliteDb;

    public GeoFlowSQLiteDb(Context context) {
        super(context, DB_FILENAME, null, DB_VERSION);
        this.mSqliteDb = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "onCreate: ");
        try {
            this.mSqliteDb = sqLiteDatabase;
            createTables();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveUserRecord(GeoFlowUserRecord user) {
        Log.d(TAG, "saveUserRecord: " + user.email);
        String query = "INSERT OR REPLACE INTO Users (" +
                "userId, " +
                "email, suspended, name, phoneNumber, " +
                "publicCertForEncryption, publicKeyForSigning, privateKeyForSigning, " +
                "createDate, lastLoginDate, loginCount, hmiMode, pwdSalt, pwdSalt2, pwdHash" +
                ") VALUES (" +
                GeoFlowUtils.toSqlString(user.userId) + "," +
                GeoFlowUtils.toSqlString(user.email) + "," +
                (user.suspended ? 1 : 0) + ", " +
                GeoFlowUtils.toSqlString(user.name) + ", " +
                GeoFlowUtils.toSqlString(user.phoneNumber) + ", " +
                GeoFlowUtils.toSqlStringSqlite(user.publicKeyForEncryption) + ", " +
                GeoFlowUtils.toSqlStringSqlite(user.publicKeyForSigning) + ", " +
                GeoFlowUtils.toSqlStringSqlite(user.privateKeyForSigning) + ", " +
                GeoFlowUtils.toSqlString(user.createDate) + ", " +
                GeoFlowUtils.toSqlString(user.lastLoginDate) + ", " +
                user.loginCount + ", " +
                GeoFlowUtils.toSqlString(user.hmiMode.toString()) + ", " +
                GeoFlowUtils.toSqlStringSqlite(user.pwdSalt) + ", " +
                GeoFlowUtils.toSqlStringSqlite(user.pwdSalt2) + ", " +
                GeoFlowUtils.toSqlStringSqlite(user.pwdHash) +
                ") ";
        this.mSqliteDb.execSQL(query);
        Log.d(TAG, "saveUserRecord: " + query);
    }

    private boolean hasUser() {
        try (Cursor cursor = this.mSqliteDb.rawQuery("SELECT COUNT(*) FROM Users", null)) {
            cursor.moveToFirst();
            int cnt = cursor.getInt(0);
            if (cnt != 0) {
                Log.d(TAG, "hasUser: true");
                return true;
            }
            Log.d(TAG, "hasUser: false");
            return false;
        }
    }

    public GeoFlowUserRecord loadUser() {
        if (hasUser()) {
            try (Cursor cursor = this.mSqliteDb.rawQuery(
                    "SELECT userId, email, suspended, name, phoneNumber, carModel, licensePlateCountry, licensePlate, obuId, " +
                            "publicCertForEncryption, publicKeyForSigning, privateKeyForSigning, confirmRegDate, confirmRegKey, " +
                            "createDate, lastLoginDate, loginCount, hmiMode, pwdSalt, pwdSalt2, pwdHash, admin " +
                            "FROM Users WHERE name=" + GeoFlowUtils.toSqlString("Polestar AAOS"), null
            )) {
                if (cursor.moveToFirst()) {
                    GeoFlowUserRecord user = new GeoFlowUserRecord();
                    user.userId = cursor.getString(cursor.getColumnIndex("userId"));
                    user.email = cursor.getString(cursor.getColumnIndex("email"));
                    user.name = cursor.getString(cursor.getColumnIndex("name"));
                    user.privateKeyForSigning = cursor.getBlob(cursor.getColumnIndex("privateKeyForSigning"));
                    user.publicKeyForSigning = cursor.getBlob(cursor.getColumnIndex("publicKeyForSigning"));
                    user.publicKeyForEncryption = cursor.getBlob(cursor.getColumnIndex("publicCertForEncryption"));
                    Log.d(TAG, "loadUser: " + user.userId);
                    return user;
                }
            }
        }

        Log.d(TAG, "loadUser: NULL");
        return null;
    }

    public void saveVehicleRecord(GeoFlowVehicleRecord vehicle) {
        Log.d(TAG, "saveVehicleRecord: " + vehicle.vehicleId + "  " + vehicle.licensePlate);
        String query = "INSERT OR REPLACE INTO Vehicles (" +
                "vehicleId, " +
                "carModel, licensePlateCountry, licensePlate, obuId, vehicleClass, sharedVehicle, createdByUserId, " +
                "createDate" +
                ") VALUES (" +
                GeoFlowUtils.toSqlString(vehicle.vehicleId) + "," +
                GeoFlowUtils.toSqlString(vehicle.carModel) + ", " +
                GeoFlowUtils.toSqlString(vehicle.licensePlateCountry) + ", " +
                GeoFlowUtils.toSqlString(vehicle.licensePlate) + ", " +
                GeoFlowUtils.toSqlString(vehicle.obuId) + ", " +
                GeoFlowUtils.toSqlString(vehicle.vehicleClass) + ", " +
                GeoFlowUtils.toSqlString(vehicle.sharedVehicle) + ", " +
                GeoFlowUtils.toSqlString(vehicle.createdByUserId) + ", " +
                GeoFlowUtils.toSqlString(vehicle.createDate) +
                ") ";
        this.mSqliteDb.execSQL(query);
//      Log.d(TAG, "saveVehicleRecord: " + query);
    }

    private boolean hasVehicle() {
        try (Cursor cursor = this.mSqliteDb.rawQuery("SELECT COUNT(*) FROM Vehicles", null)) {
            cursor.moveToFirst();
            int cnt = cursor.getInt(0);
            if (cnt != 0) {
                Log.d(TAG, "hasVehicle: true");
                return true;
            }
        }
        Log.d(TAG, "hasVehicle: false");
        return false;
    }

    public GeoFlowVehicleRecord loadVehicle() {
        String sqlStr = "SELECT vehicleId, " +
                    "carModel, licensePlateCountry, licensePlate, obuId, vehicleClass, vehicleMode, sharedVehicle, createdByUserId, " +
                    "createDate, updateDate " +
                    "FROM Vehicles " +
                    "WHERE obuId = " + GeoFlowUtils.toSqlString(GeoFlowAccountUtils.polestarObuId) + " " +
                    "ORDER BY createDate DESC";
        try (Cursor cursor = this.mSqliteDb.rawQuery(sqlStr, null)) {
            if (cursor.moveToFirst()) {
                GeoFlowVehicleRecord vehicle = new GeoFlowVehicleRecord();
                vehicle.vehicleId = cursor.getString(cursor.getColumnIndex("vehicleId"));
                vehicle.licensePlate = cursor.getString(cursor.getColumnIndex("licensePlate"));
                vehicle.carModel = cursor.getString(cursor.getColumnIndex("carModel"));
                vehicle.obuId = cursor.getString(cursor.getColumnIndex("obuId"));
                Log.d(TAG, "loadVehicle: " + vehicle.licensePlate + "  id: " + vehicle.vehicleId);
                return vehicle;
            }
        }
        Log.d(TAG, "loadVehicle: ObuId " + GeoFlowAccountUtils.polestarObuId + " not found.");
        return null;
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        onCreate(sqLiteDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void createTables() throws Exception {
        Log.d(TAG, "createTables: ");
        // Create Private Vault.
        this.mSqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS PrivateVault (\n"
                        + "	seqNum      INTEGER PRIMARY KEY NOT NULL,\n"
                        + "	hash        BLOB     NOT NULL,\n"
                        + "	data        BLOB     NOT NULL,\n"
                        + "	signature   BLOB     NOT NULL\n"
                        + ");"
        );

        // Create Vault Item Sequence Number
        this.mSqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS VaultItemSequenceNumber (\n"
                        + "	CurrentValue INTEGER\n"
                        + ");"
        );

        //Users
        this.mSqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS Users ( " +
                        "userId TEXT NOT NULL PRIMARY KEY, " +
                        "email TEXT NOT NULL, " +
                        "suspended INTEGER not null, " +
                        "name TEXT, " +
                        "phoneNumber TEXT, " +
                        "carModel TEXT, " +
                        "licensePlateCountry TEXT, " +
                        "licensePlate TEXT, " +
                        "obuId TEXT, " +
                        "publicCertForEncryption BLOB, " +
                        "publicKeyForSigning BLOB, " +
                        "privateKeyForSigning BLOB, " +
                        "createDate TEXT, " +
                        "lastLoginDate TEXT NULL, " +
                        "loginCount INTEGER, " +
                        "confirmRegDate TEXT NULL, " +
                        "confirmRegKey TEXT NULL, " +
                        "hmiMode TEXT NULL, " +
                        "pwdSalt BLOB, " +
                        "pwdSalt2 BLOB, " +
                        "pwdHash BLOB, " +
                        "admin INTEGER NULL, " +
                        "uploaded TEXT NULL " +
                        ")"
        );

        // Create Vehicle table
        this.mSqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS Vehicles ( " +
                        "vehicleId TEXT NOT NULL PRIMARY KEY, " +
                        "carModel TEXT, " +
                        "licensePlateCountry TEXT, " +
                        "licensePlate TEXT, " +
                        "obuId TEXT, " +
                        "vehicleClass TEXT, " +
                        "vehicleMode INTEGER, " +
                        "sharedVehicle INTEGER, " +
                        "createdByUserId TEXT, " +
                        "createDate TEXT, " +
                        "updateDate TEXT, " +
                        "uploaded TEXT " +
                        ")"
        );

        this.mSqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS LastKnownGoodPosition ( " +
                        "longitude     REAL NOT NULL, " +
                        "latitude      REAL NOT NULL, " +
                        "altitude      REAL NOT NULL, " +
                        "heading       REAL NOT NULL, " +
                        "speed         REAL NOT NULL, " +
                        "dateTime      TEXT NOT NULL " +
                        ")"
        );

        this.mSqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS InvoiceItems ( " +
                        "seqNum      INTEGER PRIMARY KEY NOT NULL, " +
                        "hash        BLOB NOT NULL, " +
                        "distance    REAL NOT NULL, " +
                        "amount      REAL NOT NULL, " +
                        "currency    TEXT NOT NULL, " +
                        "signature   BLOB NOT NULL " +
                        ")"
        );

        this.mSqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS InvoiceSummary ( " +
                        "seqNum        INTEGER PRIMARY KEY NOT NULL, " +
                        "itsStationId  TEXT NOT NULL, " +
                        "userName      TEXT NOT NULL, " +
                        "hash          BLOB NOT NULL, " +
                        "fromDate      TEXT NOT NULL, " +
                        "toDate        TEXT NOT NULL, " +
                        "amount        REAL NOT NULL, " +
                        "currency      TEXT NOT NULL, " +
                        "signature     BLOB NOT NULL, " +
                        "uploaded      TEXT     NULL " +
                        ")"
        );

        //Zonepackage
        this.mSqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS ZonePackage ( " +
                        "seqNum        INTEGER PRIMARY KEY NOT NULL, " +
                        "dateTime      TEXT NOT NULL, " +
                        "nvdbDownload  TEXT     NULL, " +
                        "latestChange  TEXT NOT NULL, " +
                        "nvdbSource    TEXT NOT NULL, " +
                        "body          BLOB NOT NULL, " +
                        "signature     BLOB     NULL " +
                        ")"
        );

        // Create VehicleClasses.
        this.mSqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS VehicleClasses (\n"
                        + "	classId      INTEGER PRIMARY KEY NOT NULL,\n"
                        + "	className    TEXT                NOT NULL,\n"
                        + "	description  TEXT                    NULL\n"
                        + ");"
        );
        this.mSqliteDb.execSQL("INSERT OR REPLACE INTO VehicleClasses (classId, className, description) VALUES ( 1, \"Class 1\" , \"Research group purpose (1)\")");
        this.mSqliteDb.execSQL("INSERT OR REPLACE INTO VehicleClasses (classId, className, description) VALUES ( 2, \"Class 2\" , \"Research group purpose (2)\")");

        //Holidaylist
        // Create Holiday list
        this.mSqliteDb.execSQL(
                "CREATE TABLE IF NOT EXISTS Holidays ( "
                        + "date TEXT  NOT NULL, "
                        + "info TEXT  NOT NULL"
                        + ");"
        );

        fillHolidayTable();
    }

    private void fillHolidayTable() throws Exception {
        int cnt = 0;
        try (Cursor cursor = this.mSqliteDb.rawQuery("SELECT COUNT(*) FROM Holidays", null)) {
            cursor.moveToFirst();
            cnt = cursor.getInt(0);
        }
        Log.d(TAG, "fillHolidayTable: cnt " + cnt);
        if (cnt == 0) {
            // 2021
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-01-01', '1. nyttårsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-03-28', 'Palmesøndag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-04-01', 'Skjærtorsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-04-02', 'Langfredag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-04-04', '1. påskedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-04-05', '2. påskedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-05-01', 'Offentlig høytidsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-05-13', 'Kristi Himmelfartsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-05-17', 'Grunnlovsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-05-23', '1. pinsedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-05-24', '2. pinsedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-12-25', '1. juledag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2021-12-26', '2. juledag') ");

            // 2022
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-01-01', '1. nyttårsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-04-10', 'Palmesøndag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-04-14', 'Skjærtorsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-04-15', 'Langfredag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-04-17', '1. påskedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-04-18', '2. påskedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-05-01', 'Offentlig høytidsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-05-17', 'Grunnlovsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-05-26', 'Kristi Himmelfartsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-06-05', '1. pinsedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-06-06', '2. pinsedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-12-25', '1. juledag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2022-12-26', '2. juledag') ");

            // 2023
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-01-01', '1. nyttårsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-04-02', 'Palmesøndag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-04-06', 'Skjærtorsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-04-07', 'Langfredag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-04-09', '1. påskedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-04-10', '2. påskedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-05-01', 'Offentlig høytidsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-05-17', 'Grunnlovsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-05-18', 'Kristi Himmelfartsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-05-28', '1. pinsedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-05-29', '2. pinsedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-12-25', '1. juledag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2023-12-26', '2. juledag') ");

            // 2024
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-01-01', '1. nyttårsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-03-24', 'Palmesøndag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-03-28', 'Skjærtorsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-03-29', 'Langfredag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-03-31', '1. påskedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-04-01', '2. påskedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-05-01', 'Offentlig høytidsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-05-09', 'Kristi Himmelfartsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-05-17', 'Grunnlovsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-05-19', '1. pinsedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-05-20', '2. pinsedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-12-25', '1. juledag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2024-12-26', '2. juledag') ");

            // 2025
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-01-01', '1. nyttårsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-04-13', 'Palmesøndag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-04-17', 'Skjærtorsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-04-18', 'Langfredag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-04-20', '1. påskedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-04-21', '2. påskedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-05-01', 'Offentlig høytidsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-05-17', 'Grunnlovsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-05-29', 'Kristi Himmelfartsdag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-06-08', '1. pinsedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-06-09', '2. pinsedag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-12-25', '1. juledag') ");
            this.mSqliteDb.execSQL("INSERT INTO Holidays (date, info) VALUES ('2025-12-26', '2. juledag') ");
        }
    }

    public void saveZonesToSqlLite(GeoFlowZonePackage pkg) throws Exception {
        Log.d(TAG, "saveZonesToSqlLite: ");
        String sqlStr = "INSERT INTO ZonePackage (dateTime, nvdbDownload, latestChange, nvdbSource, body, signature) VALUES(?,?,?,?,?,?)";
        try (SQLiteStatement statement = this.mSqliteDb.compileStatement(sqlStr)) {
            statement.bindString(1, GeoFlowUtils.toSqlStringPlain(LocalDateTime.now()));
            statement.bindString(2, GeoFlowUtils.toSqlStringPlain(pkg.nvdbDownload));
            statement.bindString(3, GeoFlowUtils.toSqlStringPlain(pkg.latestChange));
            statement.bindString(4, pkg.nvdbSource);
            if (pkg.body != null) {
                statement.bindBlob(5, pkg.body);
            }
            if (pkg.signature != null) {
                statement.bindBlob(6, pkg.signature);
            }
            long rowId = statement.executeInsert();
        }
    }

    public GeoFlowZonePackage loadZonesFromSqLite() throws Exception {
        Log.d(TAG, "loadZonesFromSqLite: ");
        String sql = "SELECT MAX(dateTime) FROM ZonePackage";
        String latestDate = null;
        try (Cursor cursor = this.mSqliteDb.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                latestDate = cursor.getString(0);
            }
            Log.d(TAG, "loadZonesFromSqLite: latestDate: " + latestDate);
        }

        if (latestDate == null) {
            throw new Exception("No data found in ZonePackage table (1)");
        }

        sql = "SELECT dateTime, nvdbDownload, latestChange, nvdbSource, body, signature " +
                "FROM ZonePackage WHERE dateTime = '" + latestDate + "'";
//
        try (Cursor cursor = this.mSqliteDb.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                GeoFlowZonePackage pkg = new GeoFlowZonePackage();
                pkg.nvdbDownload = GeoFlowUtils.toLocalDateTime(cursor.getString(cursor.getColumnIndex("nvdbDownload")));
                pkg.latestChange = GeoFlowUtils.toLocalDateTime(cursor.getString(cursor.getColumnIndex("latestChange")));
                pkg.nvdbSource = cursor.getString(cursor.getColumnIndex("nvdbSource"));
                pkg.body = cursor.getBlob(cursor.getColumnIndex("body"));
                pkg.signature = cursor.getBlob(cursor.getColumnIndex("signature"));
                return pkg;
            }
        }
        throw new Exception("No data found in ZonePackage table (2)");
    }

    public void savePrivateVaultRecord(GeoFlowPrivateVaultRecordSecured recordSecured) throws Exception {
        String sqlStr = "INSERT INTO PrivateVault (hash, data, signature) VALUES(?,?,?)";
        try (SQLiteStatement statement = this.mSqliteDb.compileStatement(sqlStr)) {
            statement.bindBlob(1, recordSecured.hash);
            statement.bindBlob(2, recordSecured.data);
            statement.bindBlob(3, recordSecured.signature);
            long rowId = statement.executeInsert();
        }
    }

    public void saveInvoiceItem(GeoFlowInvoiceItem invoiceItem) throws Exception {
        String sqlStr = "INSERT INTO InvoiceItems (hash, amount, distance, currency, signature) VALUES(?,?,?,?,?)";
        try (SQLiteStatement statement = this.mSqliteDb.compileStatement(sqlStr)){
            statement.bindBlob(1, invoiceItem.hash);
            statement.bindDouble(2, invoiceItem.amount);
            statement.bindDouble(3, invoiceItem.distance);
            statement.bindString(4, invoiceItem.currency);
            statement.bindBlob(5, invoiceItem.signature);
            long rowId = statement.executeInsert();
        }
    }

    public long getNextVaultItemSequenceNumber() throws Exception {
        int next = -1;
        String sqlStr = "SELECT CurrentValue FROM VaultItemSequenceNumber";
        try (Cursor cursor = this.mSqliteDb.rawQuery(sqlStr, null)) {
            if (cursor.moveToFirst()) {
                next = cursor.getInt(cursor.getColumnIndex("CurrentValue"));
            }
        }
        if (next == -1){
            sqlStr = "INSERT INTO VaultItemSequenceNumber (CurrentValue) VALUES (2)";
            try (SQLiteStatement statement = this.mSqliteDb.compileStatement(sqlStr)) {
                statement.executeInsert();
            }
            return 1;
        } else {
            next++;
            sqlStr = "UPDATE VaultItemSequenceNumber SET CurrentValue = " + next;
            try (SQLiteStatement statement = this.mSqliteDb.compileStatement(sqlStr)){
                statement.executeInsert();
            }
        }
        return next;
    }

    public ArrayList<GeoFlowInvoiceItem> getInvoiceItems() {
        String sqlStr = "SELECT hash, amount, currency FROM InvoiceItems ORDER BY seqNum";
        ArrayList<GeoFlowInvoiceItem> invoiceItems = new ArrayList<>();
        try (Cursor cursor = this.mSqliteDb.rawQuery(sqlStr, null)){
            while (cursor.moveToNext()){
                GeoFlowInvoiceItem invoiceItem = new GeoFlowInvoiceItem();
                invoiceItem.hash = cursor.getBlob(cursor.getColumnIndex("hash"));
                invoiceItem.amount = cursor.getDouble(cursor.getColumnIndex("amount"));
                invoiceItem.currency = cursor.getString(cursor.getColumnIndex("currency"));
                invoiceItems.add(invoiceItem);
            }
        }
        return invoiceItems;
    }

    public int saveInvoiceSummary(GeoFlowInvoiceSummary invoiceSummary) throws Exception {
        String sqlStr = "INSERT INTO InvoiceSummary " +
                "(itsStationId, userName, hash, fromDate, toDate, amount, currency, signature, uploaded)" +
                " VALUES(?,?,?,?,?,?,?,?,NULL)";
        try (SQLiteStatement statement = this.mSqliteDb.compileStatement(sqlStr)) {
            statement.bindString(1, invoiceSummary.itsStationId);
            statement.bindString(2, invoiceSummary.userName);
            statement.bindBlob(3, invoiceSummary.hash);
            statement.bindString(4, GeoFlowUtils.toSqlStringPlain(invoiceSummary.fromDate));
            statement.bindString(5, GeoFlowUtils.toSqlStringPlain(invoiceSummary.toDate));
            statement.bindDouble(6, invoiceSummary.amount);
            statement.bindString(7, invoiceSummary.currency);
            statement.bindBlob(8, invoiceSummary.signature);
            return (int) statement.executeInsert();
        }
    }

    public void deleteInvoiceItems() {
        String sqlStr = "DELETE FROM InvoiceItems";
        this.mSqliteDb.execSQL(sqlStr);
    }
}
