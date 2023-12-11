package com.qfree.its.iso21177poc.common.geoflow.thick_client;

import android.util.Log;

import com.qfree.geoflow.privatevault.GeoFlowPrivateVaultService;
import com.qfree.geoflow.privatevault.PrivateVaultState;
import com.qfree.geoflow.toll.api.GeoFlowEnforcementRecord;
import com.qfree.geoflow.toll.api.GeoFlowInvoiceItem;
import com.qfree.geoflow.toll.api.GeoFlowInvoiceSummary;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecord;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordItem;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordItemInvoiceItem;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordItemPos;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordItemUploadStatus;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordItemUserId;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordItemVehicleId;
import com.qfree.geoflow.toll.api.GeoFlowPrivateVaultRecordSecured;
import com.qfree.geoflow.toll.api.GeoFlowServerAccessRecord;
import com.qfree.geoflow.toll.api.GeoFlowTollingState;
import com.qfree.geoflow.toll.api.GeoFlowUserRecord;
import com.qfree.geoflow.toll.api.GeoFlowUtils;
import com.qfree.geoflow.toll.api.GeoFlowVehicleRecord;
import com.qfree.geoflow.toll.api.GeoFlowWsApiEventEnforcement;
import com.qfree.geoflow.toll.api.GeoFlowWsApiEventPower;
import com.qfree.geoflow.toll.misc.OsgiBundleInfo;
import com.qfree.its.iso21177poc.common.geoflow.EncryptionKeyUtils;
import com.qfree.its.iso21177poc.common.geoflow.GeoFlowSQLiteDb;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.FileLogger;
import com.qfree.its.iso21177poc.common.geoflow.thin_client.LogEvents;
import com.qfree.its.location.GeoCalculations;
import com.qfree.its.location.Position;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Locale;

/*Implements android.database.sqlite api for saving data to database, incompatible with  java.sql.Connection*/
public class PrivateVaultImpl implements GeoFlowPrivateVaultService {
    private static final String TAG = PrivateVaultImpl.class.getSimpleName();

    private final int mCurrentItemsMaxCount = 1000;
    private final int mCurrentItemMaxSeconds = 60;

    private final GeoFlowSQLiteDb mGeoFlowSQLiteDb;
    private int mSequenceNumer = 1;
    private ArrayList<GeoFlowPrivateVaultRecordItem> mCurrentItems;
    private GeoFlowUserRecord mUser;
    private GeoFlowVehicleRecord mVehicle;
    private PrivateKey mPrivateKeyForSigning;
    private PublicKey mPublicEncryptionKey;
    private GeoFlowTollingState mTollingState;
    private long mItemCnt = 0;
    private long mFlushCnt = 0;
    private long mItemsDeletedCnt;

    public PrivateVaultImpl(GeoFlowSQLiteDb geoFlowSQLiteDb) {
        this.mGeoFlowSQLiteDb = geoFlowSQLiteDb;
        this.mCurrentItems = new ArrayList<>();
    }

    @Override
    public void initialize(Connection connection) throws Exception {
        if (getUser() != null && getVehicle() != null) {
            initialize(connection, getUser(), getVehicle());
        }
    }

    @Override
    public void initialize(Connection connection, GeoFlowUserRecord geoFlowUserRecord, GeoFlowVehicleRecord geoFlowVehicleRecord) throws Exception {
        Log.d(TAG, "initialize: " + geoFlowUserRecord.name + " " + geoFlowVehicleRecord.licensePlate);
        this.mUser = geoFlowUserRecord;
        this.mVehicle = geoFlowVehicleRecord;
        FileLogger.logEvent(LogEvents.PRIVATE_VAULT,
                String.format(Locale.ROOT, "Initialize;%s;%s", geoFlowUserRecord.name, geoFlowVehicleRecord.licensePlate));

        this.mPrivateKeyForSigning = EncryptionKeyUtils.byteArrayToECPrivateKey(this.mUser.privateKeyForSigning);
        this.mPublicEncryptionKey = EncryptionKeyUtils.byteArrayToRSAPublicKey(this.mUser.publicKeyForEncryption);
        FileLogger.logEvent(LogEvents.PRIVATE_VAULT,
                String.format(Locale.ROOT, "PrivateKey;%s;%s",
                        this.mPrivateKeyForSigning.getAlgorithm(), this.mPrivateKeyForSigning.getFormat()));
        FileLogger.logEvent(LogEvents.PRIVATE_VAULT,
                String.format(Locale.ROOT, "PublicKey;%s",
                        GeoFlowUtils.bin2hex(this.mUser.publicKeyForEncryption)));
        FileLogger.logEvent(LogEvents.PRIVATE_VAULT,
                String.format(Locale.ROOT, "PublicKey;%s;%s",
                        this.mPublicEncryptionKey.getAlgorithm(), this.mPublicEncryptionKey.getFormat()));
    }

    @Override
    public void append(Connection connection, Position position) throws Exception {
        GeoFlowPrivateVaultRecordItemPos vaultRecord = new GeoFlowPrivateVaultRecordItemPos();
        vaultRecord.timestamp = LocalDateTime.now();
        vaultRecord.sequenceNumber = this.mSequenceNumer++;
        vaultRecord.latitude = position.getLatitude();
        vaultRecord.latitude = position.getLongitude();
        saveToCache(vaultRecord);
    }

    @Override
    public void append(Connection connection, GeoFlowWsApiEventPower geoFlowWsApiEventPower) throws Exception {
        //TODO: Not relevant?
    }

    @Override
    public void append(Connection connection, GeoFlowWsApiEventEnforcement geoFlowWsApiEventEnforcement) throws Exception {
        //TODO: Not relevant?
    }

    @Override
    public void append(Connection connection, GeoFlowEnforcementRecord geoFlowEnforcementRecord) throws Exception {
        //TODO: Not relevant?
    }

    @Override
    public void append(Connection connection, GeoFlowInvoiceItem geoFlowInvoiceItem) throws Exception {
        GeoFlowPrivateVaultRecordItemInvoiceItem invoiceItem = createVaultInvoiceItem(geoFlowInvoiceItem);
        saveToCache(invoiceItem);
    }

    @Override
    public void append(Connection connection, GeoFlowInvoiceSummary geoFlowInvoiceSummary) throws Exception {

    }

    @Override
    public void append(Connection connection, GeoFlowServerAccessRecord geoFlowServerAccessRecord) throws Exception {
        //TODO: Not relevant?
    }

    @Override
    public void append(Connection connection, GeoFlowPrivateVaultRecordItemUploadStatus geoFlowPrivateVaultRecordItemUploadStatus) throws Exception {
        //TODO: Not relevant?
    }

    @Override
    public void append(Connection connection, OsgiBundleInfo osgiBundleInfo) throws Exception {
        //TODO: Not relevant?
    }

    @Override
    public void encryptAndSign(Connection connection, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        if (this.mCurrentItems == null || this.mCurrentItems.isEmpty()) {
            return;
        }
        if (this.mPrivateKeyForSigning == null) {
            this.mItemsDeletedCnt += this.mCurrentItems.size();
            FileLogger.logEvent(LogEvents.PRIVATE_VAULT, String.format(Locale.ROOT,
                    "%s;%s", "encryptAndSign", "No signing key, data Deleted"));
            return;
        }

        GeoFlowPrivateVaultRecordSecured recordSecured = new GeoFlowPrivateVaultRecordSecured();
        if (this.mTollingState != null) {
            GeoFlowInvoiceItem invoiceItem = new GeoFlowInvoiceItem();
            invoiceItem.hash = recordSecured.hash;
            invoiceItem.amount = mTollingState.accumulatedCost;
            invoiceItem.distance = mTollingState.accumulatedDistance;
            invoiceItem.currency = mTollingState.currency;
            mTollingState.resetState();
            mTollingState.currency = invoiceItem.currency;
            invoiceItem.createSignature(mPrivateKeyForSigning);
            GeoFlowPrivateVaultRecordItemInvoiceItem vaultInvoiceItem = createVaultInvoiceItem(invoiceItem);
            this.mCurrentItems.add(vaultInvoiceItem);
            saveToDatabase(invoiceItem);
            FileLogger.logEvent(LogEvents.PRIVATE_VAULT, String.format(Locale.ROOT,
                    "%s;%s", "encryptAndSign", "invoiceItem saved"));
        }

        GeoFlowPrivateVaultRecordItem firstItem = this.mCurrentItems.get(0);
        GeoFlowPrivateVaultRecordItem lastItem = this.mCurrentItems.get(this.mCurrentItems.size() - 1);
        GeoFlowPrivateVaultRecord vaultRecord = new GeoFlowPrivateVaultRecord();
        vaultRecord.sequenceNumber = getNextVaultItemSequenceNumber();
        vaultRecord.startTimeUtc = firstItem.timestamp;
        vaultRecord.endTimeUtc = lastItem.timestamp;
        vaultRecord.entries = mCurrentItems;

        byte[] vaultRecordPlain = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)){
            objectOutputStream.writeObject(vaultRecord);
            objectOutputStream.flush();
            vaultRecordPlain = byteArrayOutputStream.toByteArray();
        }
        recordSecured.signature = GeoFlowUtils.generateECDSASignature(privateKey, vaultRecordPlain);
        recordSecured.data = GeoFlowUtils.encrypt(publicKey, vaultRecordPlain);
        saveToDatabase(recordSecured);
        FileLogger.logEvent(LogEvents.PRIVATE_VAULT, String.format(Locale.ROOT,
                "%s;%s", "encryptAndSign", "records stored"));
    }

    private GeoFlowPrivateVaultRecordItemInvoiceItem createVaultInvoiceItem(GeoFlowInvoiceItem invoice) throws Exception {
        GeoFlowPrivateVaultRecordItemInvoiceItem invoiceItem = new GeoFlowPrivateVaultRecordItemInvoiceItem();
        invoiceItem.timestamp = LocalDateTime.now();
        invoiceItem.sequenceNumber = this.mSequenceNumer++;
        invoiceItem.hash = invoice.hash;
        invoiceItem.amount = invoice.amount;
        invoiceItem.distance = invoice.distance;
        invoiceItem.currency = invoice.currency;
        invoiceItem.signature = invoice.signature;
        return invoiceItem;
    }

    private long getNextVaultItemSequenceNumber() throws Exception {
        long next = this.mGeoFlowSQLiteDb.getNextVaultItemSequenceNumber();
        FileLogger.logEvent(LogEvents.PRIVATE_VAULT, String.format(Locale.ROOT,
                "%s;%d", "getNextVaultItemSequenceNumber", next));
        return next;
    }

    @Override
    public void flushItems(Connection connection) throws Exception {
        flushItems();
    }

    private void flushItems() throws Exception {
        Log.d(TAG, "flushItems: cnt " + mCurrentItems.size());
        if (mCurrentItems.size() > 0) {
            GeoFlowPrivateVaultRecordItem firstItem = mCurrentItems.get(0);
            GeoFlowPrivateVaultRecordItem lastItem = mCurrentItems.get(mCurrentItems.size() - 1);
            long deltaTime = lastItem.timestamp.toEpochSecond(ZoneOffset.UTC) - firstItem.timestamp.toEpochSecond(ZoneOffset.UTC);
            int posCnt = 0;
            double deltaPos = 0.0;
            GeoFlowPrivateVaultRecordItemPos prevPos = null;
            for (GeoFlowPrivateVaultRecordItem item : mCurrentItems) {
                if (item instanceof GeoFlowPrivateVaultRecordItemPos) {
                    GeoFlowPrivateVaultRecordItemPos pos = (GeoFlowPrivateVaultRecordItemPos) item;
                    if (prevPos != null) {
                        double delta = GeoCalculations.calcDistance(prevPos.latitude, prevPos.longitude, pos.latitude, pos.longitude);
                        deltaPos += delta;
                        posCnt++;
                    }
                    prevPos = pos;
                }
            }
            Log.d(TAG, "flushItems: " + String.format(Locale.ROOT,
                    "PosCnt=%d Dist=%.2f m, DeltaTime=%d sec", posCnt, deltaPos, deltaTime));
            FileLogger.logEvent(LogEvents.PRIVATE_VAULT,
                    String.format(Locale.ROOT, "FlushItems;%d;%d;%.2f;%d",
                            mCurrentItems.size(), posCnt, deltaPos, deltaTime));
        }
        mFlushCnt++;
        encryptAndSign(null, mPublicEncryptionKey, mPrivateKeyForSigning);
        mCurrentItems.clear();
        initializeItemList(getUser(), getVehicle());
    }

    private void saveToCache(GeoFlowPrivateVaultRecordItem vaultRecord) throws Exception {
        mItemCnt++;
        mCurrentItems.add(vaultRecord);
        if (mCurrentItems.size() > mCurrentItemsMaxCount) {
            flushItems();
            return;
        }
        GeoFlowPrivateVaultRecordItem firstItem = mCurrentItems.get(0);
        GeoFlowPrivateVaultRecordItem lastItem = vaultRecord;
        long deltaTime = lastItem.timestamp.toEpochSecond(ZoneOffset.UTC) - firstItem.timestamp.toEpochSecond(ZoneOffset.UTC);
        if (deltaTime > mCurrentItemMaxSeconds) {
            flushItems();
            return;
        }
    }

    private void saveToDatabase(GeoFlowPrivateVaultRecordSecured recordSecured) throws Exception {
        this.mGeoFlowSQLiteDb.savePrivateVaultRecord(recordSecured);
    }

    private void saveToDatabase(GeoFlowInvoiceItem invoiceItem) throws Exception {
        this.mGeoFlowSQLiteDb.saveInvoiceItem(invoiceItem);
    }

    private void initializeItemList(GeoFlowUserRecord user, GeoFlowVehicleRecord vehicle) {
        if (user != null) {
            this.mCurrentItems.add(new GeoFlowPrivateVaultRecordItemUserId(user));
        }
        if (vehicle != null) {
            this.mCurrentItems.add(new GeoFlowPrivateVaultRecordItemVehicleId(vehicle));
        }
    }

    //TODO: createSignature
    @Override
    public byte[] createSignature(byte[] bytes) throws Exception {
        if (this.mPrivateKeyForSigning == null){
            throw new Exception("Private key is not set");
        }
        return GeoFlowUtils.generateECDSASignature(mPrivateKeyForSigning, bytes);
    }

    @Override
    public void updateState(GeoFlowTollingState geoFlowTollingState) {
        this.mTollingState = geoFlowTollingState;
    }

    @Override
    public GeoFlowUserRecord getUser() {
        return this.mUser;
    }

    public void setUser(GeoFlowUserRecord user) {
        this.mUser = user;
    }

    @Override
    public GeoFlowVehicleRecord getVehicle() {
        return this.mVehicle;
    }

    public void setVehicle(GeoFlowVehicleRecord vehicle) {
        this.mVehicle = vehicle;
    }

    @Override
    public PrivateVaultState getState() {
        PrivateVaultState state = new PrivateVaultState();
        state.user = getUser();
        state.vehicle = getVehicle();
        state.sequenceNumber = mSequenceNumer;
        state.publicEncryptionKey = mPublicEncryptionKey;
        state.currentItemCount = mCurrentItems.size();
        if (mTollingState != null) {
            state.accumulatedCost = mTollingState.accumulatedCost;
            state.accumulatedDistance = mTollingState.accumulatedDistance;
        } else {
            state.accumulatedCost = -1;
            state.accumulatedDistance = -1;
        }
        state.flushCount = mFlushCnt;
        state.itemCnt = mItemCnt;
        state.itemsDeletedCnt = mItemsDeletedCnt;
        return state;
    }

    @Override
    public int getMaxTimespan() {
        return mCurrentItemMaxSeconds;
    }

    @Override
    public int getMaxCount() {
        return mCurrentItemsMaxCount;
    }
}
