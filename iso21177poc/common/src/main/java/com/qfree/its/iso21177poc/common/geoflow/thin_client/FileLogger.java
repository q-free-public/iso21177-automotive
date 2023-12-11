package com.qfree.its.iso21177poc.common.geoflow.thin_client;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.qfree.geoflow.toll.api.GeoFlowUserRecord;
import com.qfree.geoflow.toll.api.GeoFlowVehicleRecord;
import com.qfree.geoflow.toll.api.LastKnownGoodPosition;
import com.qfree.its.iso21177poc.common.geoflow.Config;
import com.qfree.its.location.Position;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class FileLogger {
	private static final String TAG = FileLogger.class.getSimpleName();
	private static Context mContext;
	private static String mAppVersion;
	private static GeoFlowUserRecord mUser;
	private static GeoFlowVehicleRecord mVehicle;
	private static String      mVehicleIdStr = null;

	private static File        logDir = null;
	private static PrintWriter activePrintWriter = null;
	private static File        activeFile = null;
	private static long        nanoTickLogStarted = 0;
	private static int         sequenceNumber;
	private static String      curUploadTopic;
	private static File        curUploadFile = null;

	static final long   maxDurationInSeconds = 60 * 60 * 4L; // 4 hours.

	public static void setContext(Context context) {
		mContext = context;
	}

	public static void log(String text) {
		logEvent(LogEvents.UNDEFINED, text);
	}

	public static void setInfo(GeoFlowVehicleRecord vehicle, GeoFlowUserRecord user, String appVersion) {
		if (mUser == null) {
			if (user != null && user.userId != null){
				logEvent(LogEvents.USER_ID, user.userId);
			}
		}
		mUser = user;

		if (mVehicle == null) {
			if (vehicle != null && vehicle.vehicleId != null){
				logEvent(LogEvents.VEHICLE_ID, vehicle.vehicleId);
			}
		}
		mVehicle = vehicle;
		if (mVehicle != null && mVehicle.licensePlate != null){
			mVehicleIdStr = vehicle.licensePlate;
		} else {
			mVehicleIdStr = "INIT";
		}

		if (appVersion != null) {
			logEvent(LogEvents.APP_VERSION, appVersion);
		}
		mAppVersion = appVersion;
	}

	private static void openLogFile() throws Exception {
		if (activePrintWriter != null)
			return;

		String rootDir = Config.USERDATA_DIR;
		String subDir = Config.LOG_DIR;

		// Init log directory
		File userdataDir = mContext.getDir(rootDir, Context.MODE_PRIVATE);
		if (!userdataDir.exists()){
			userdataDir.mkdir();
		}
		logDir = new File(userdataDir, subDir);
		logDir.mkdirs();

		LocalDateTime now = LocalDateTime.now();
		activeFile = new File(logDir, LogFormatStrings.dateFormat.format(now) + ".csv");
		activePrintWriter = new PrintWriter(activeFile);
		sequenceNumber = 0;
		nanoTickLogStarted  = System.nanoTime();
		Log.d(TAG, "GeoFlow.ThinClient: Using file " + activeFile.getAbsolutePath());

		// Adding user id and vehicle id
		if (mUser != null && mUser.userId != null){
			logEvent(LogEvents.USER_ID, mUser.userId);
		}
		if (mVehicle != null && mVehicle.vehicleId != null){
			logEvent(LogEvents.VEHICLE_ID, mVehicle.vehicleId);
		}

		// Add app version numbers
		if (mAppVersion != null) {
			logEvent(LogEvents.APP_VERSION, mAppVersion);
		}

		// Add full filename
		logEvent(LogEvents.FULL_FILENAME, activeFile.getAbsolutePath());
	}

    public static void logException(Exception e) {
		if (e == null) {
			logEvent(LogEvents.EXCEPTION, "null");
		} else {
			logEvent(LogEvents.EXCEPTION, e.getClass().getCanonicalName() + ": " + e.getMessage());
			if (e.getCause() != null && e.getCause() instanceof Exception) {
				logException((Exception) e.getCause());
			}
		}
	}

	synchronized public static void logEvent(LogEvents event, Object info) {
		try {
			if (activePrintWriter == null) {
				openLogFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		LocalDateTime now = LocalDateTime.now();
		long timeInSeconds = now.toEpochSecond(ZoneOffset.UTC);
		long milliSec = now.getNano() / 1000000;
		if (activePrintWriter != null && (nanoTickLogStarted != 0 && (System.nanoTime()-nanoTickLogStarted)*1e-9 > maxDurationInSeconds)) {
			// Avoid files being used forever.
			activePrintWriter.format(Locale.US, LogFormatStrings.logLineCommonFormatStr, sequenceNumber,
					timeInSeconds, milliSec,
					LogFormatStrings.dateFormatLong.format(now), mVehicleIdStr, LogEvents.CLOSING_FILE.toString());
			activePrintWriter.println("Time exceeded");

			activePrintWriter.close();
			activePrintWriter = null;
		}

		// To debug console
		Log.d(TAG, "logEvent: " + String.format(Locale.US, LogFormatStrings.logLineCommonFormatStr, sequenceNumber,
				timeInSeconds, milliSec,
				LogFormatStrings.dateFormatLong.format(now), mVehicleIdStr, event.toString()) + info);

		try {
			openLogFile();
		} catch (Exception e) {
			Log.d(TAG, "logEvent: Exception: " + e.getClass().getName() + ": " +  e.getMessage());
			return;
		}

		activePrintWriter.format(Locale.US, LogFormatStrings.logLineCommonFormatStr, sequenceNumber,
				timeInSeconds, milliSec,
				LogFormatStrings.dateFormatLong.format(now), mVehicleIdStr, event.toString());
		switch (event) {
			case GPS:
				Position pos = (Position) info;
				activePrintWriter.format(Locale.US, LogFormatStrings.logLineGPSFormatStr, pos.getLongitude(), pos.getLatitude(),
						pos.getHeight(), pos.getHeading(), pos.getVelocity(), pos.getSatelliteCount(), pos.getUbloxFixType(),
						pos.getHdop(), Math.sqrt(pos.getPositionCovarianceMatrix()[0]));
				//To console
//				Log.d(TAG, "logEvent(gps): " + String.format(Locale.US, LogFormatStrings.logLineGPSFormatStr, pos.getLongitude(), pos.getLatitude(),
//						pos.getHeight(), pos.getHeading(), pos.getVelocity(), pos.getSatelliteCount(), pos.getUbloxFixType(),
//						pos.getHdop(), Math.sqrt(pos.getPositionCovarianceMatrix()[0])));
				break;
			case LAST_KNOWN_GOOD:
				LastKnownGoodPosition lastKnown = (LastKnownGoodPosition) info;
				activePrintWriter.format(Locale.US, LogFormatStrings.logLineGPSFormatStr, lastKnown.pos.getLongitude(), lastKnown.pos.getLatitude(),
						lastKnown.pos.getHeight(), lastKnown.pos.getHeading(), lastKnown.pos.getVelocity(), 0, 0,
						0.0, 0.0);
				//To console
//				Log.d(TAG, "logEvent(lastknown): " + String.format(Locale.US, LogFormatStrings.logLineGPSFormatStr, lastKnown.pos.getLongitude(), lastKnown.pos.getLatitude(),
//						lastKnown.pos.getHeight(), lastKnown.pos.getHeading(), lastKnown.pos.getVelocity(), 0, 0,
//						0.0, 0.0));
				break;
			default:
				activePrintWriter.format(Locale.US, LogFormatStrings.logLineDefaultFormatStr, (String) info);
				//To console
//				Log.d(TAG, "logEvent(default): " + String.format(Locale.US, LogFormatStrings.logLineDefaultFormatStr, (String) info));
				break;
		}
		sequenceNumber++;
		activePrintWriter.println();
		if (sequenceNumber % 10 == 0) {
			activePrintWriter.flush();
		}
	}

	static private File createZipFile(File fileToZip) throws Exception {
		Log.d(TAG, "Zipping " + fileToZip.getCanonicalPath());
		try (FileInputStream fileInputStream = new FileInputStream(fileToZip)) {
			File zippedFile = new File(fileToZip.getCanonicalPath() + ".zip");
			try (FileOutputStream fileOutputStream = new FileOutputStream(zippedFile)) {
				ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
				ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
				zipOutputStream.putNextEntry(zipEntry);
				byte[] buffer = new byte[1024 * 8];
				int length;
				while ((length = fileInputStream.read(buffer)) > 0) {
					zipOutputStream.write(buffer, 0, length);
				}
				zipOutputStream.closeEntry();
				zipOutputStream.close();
			}

			return zippedFile;
		}
	}

	static public void zipAndPostLogFile(String url, Handler handler/*, WebRequestQueue webRequestQueue*/) throws Exception {
		File[] files = logDir.listFiles();
		for (File file : files) {
			if (file.getName().endsWith(".zip")) {
				// One ZIP file was a leftover from a previous run, try to send again.
				byte[] zipFileBinaryContent = Files.readAllBytes(Paths.get(file.getCanonicalPath()));
//				byte[] encryptedZipFileBinaryContent = encrypt(Activator.publicEncryptionKey, zipFileBinaryContent);
				curUploadTopic = Config.FILE_UPLOAD_EVENT_TOPIC + "/" + file.getName().replaceAll("\\.", "_");
				curUploadFile = file;
				// Post reports to server
				HashMap<String, String> requestProperties = new HashMap<String, String>();
				requestProperties.put("X-Qfree-ItsStation", mVehicleIdStr);
				requestProperties.put("X-Qfree-Filename", file.getName());
				requestProperties.put("X-Qfree-Source", "polestar");
				LogFilePostThread.doPost(url, zipFileBinaryContent, requestProperties, curUploadTopic, handler);
				return;
			}
		}

		// No old ZIP file found, look for CSV files.
		LogFile uploadCandicate = null;
		for (File file : files) {
			// Don't process current log file
			if (!file.getName().equals(activeFile.getName())) {
				uploadCandicate = new LogFile();
				uploadCandicate.csvFile = file;
				break;
			}
		}
		if (uploadCandicate != null) {
			uploadCandicate.zipFile = createZipFile(uploadCandicate.csvFile);
			if (uploadCandicate.zipFile != null) {
				uploadCandicate.csvFile.delete();
				byte[] zipFileBinaryContent = Files.readAllBytes(Paths.get(uploadCandicate.zipFile.getCanonicalPath()));
//				byte[] encryptedZipFileBinaryContent = encrypt(Activator.publicEncryptionKey, zipFileBinaryContent);
				curUploadTopic = Config.FILE_UPLOAD_EVENT_TOPIC + "/" + uploadCandicate.zipFile.getName().replaceAll("\\.", "_");
				curUploadFile = uploadCandicate.zipFile;
				// Post reports to server
				HashMap<String, String> requestProperties = new HashMap<String, String>();
				requestProperties.put("X-Qfree-ItsStation", mVehicleIdStr);
				requestProperties.put("X-Qfree-Filename", uploadCandicate.zipFile.getName());
				requestProperties.put("X-Qfree-Source", "polestar");
				LogFilePostThread.doPost(url, zipFileBinaryContent, requestProperties, curUploadTopic, handler);
			}
		}
	}

	public static byte[] encrypt(PublicKey publicKey, byte[] payload) throws Exception {
		// Generate a random AES key, used only for this payload
		KeyGenerator generator = KeyGenerator.getInstance("AES");
		generator.init(256); // The AES key size in number of bits
		SecretKey aesKey = generator.generateKey();

		// Encrypt payload with AES (IV is zero, however, each key is only used once)
		Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(new byte[16]));
		byte[] cipherText = aesCipher.doFinal(payload);

		// Encrypt the AES key with RSA (see https://developer.android.com/guide/topics/security/cryptography)
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
		cipher.init(Cipher.PUBLIC_KEY, publicKey, new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
		byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());

		// Merge key and payload in a byte array.
		try (ByteArrayOutputStream fos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(fos) ) {
			oos.writeObject(encryptedKey);
			oos.writeObject(cipherText);
			oos.flush();
			byte[] result = fos.toByteArray();
			return result;
		}
	}

	static public void deleteUploadedFile(String topic) {
		if (topic.equals(curUploadTopic)) {
			Log.d(TAG,"HTTP transfer done, deleting " + curUploadFile.getAbsolutePath());
			curUploadFile.delete();
			curUploadFile = null;
			curUploadTopic = null;
		} else {
			Log.d(TAG, "HTTP transfer done, but topic mismatch. Got '" + topic + "' but expected '" + curUploadTopic + "'");
		}
	}

	static public void closeLogFile(String reason) {
		if (activePrintWriter != null) {
			logEvent(LogEvents.CLOSING_FILE, reason);
			activePrintWriter.close();
			activePrintWriter = null;
		}
	}

	static public void setUser(GeoFlowUserRecord geoFlowUserRecord) {
		mUser = geoFlowUserRecord;
	}

	static public void setVehicle(GeoFlowVehicleRecord geoFlowVehicleRecord) {
		mVehicle = geoFlowVehicleRecord;
	}

	static public void setVehicleIdStr(String vehicleIdStr) {
		mVehicleIdStr = vehicleIdStr;
	}

	static public String getVehicleIdStr() {
		return mVehicleIdStr;
	}

	static public GeoFlowUserRecord getUser() {
		return mUser;
	}
}
