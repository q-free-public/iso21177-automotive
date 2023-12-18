package com.qfree.its.iso21177poc.common.geoflow.thin_client;

import android.content.Context;
import android.util.Log;

import com.qfree.its.iso21177poc.common.geoflow.Config;
import com.qfree.its.location.Position;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

public class FileLogger {
	private static final String TAG = FileLogger.class.getSimpleName();
	private static Context mContext;
	private static String mAppVersion;
	private static String      mVehicleIdStr = null;
	private static File        logDir = null;
	private static PrintWriter activePrintWriter = null;
	private static File        activeFile = null;
	private static long        nanoTickLogStarted = 0;
	private static int         sequenceNumber;
	static final long   maxDurationInSeconds = 60 * 60 * 4L; // 4 hours.

	public static void setContext(Context context) {
		mContext = context;
	}

	public static void log(String text) {
		logEvent(LogEvents.UNDEFINED, text);
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
//		Log.d(TAG, "logEvent: " + String.format(Locale.US, LogFormatStrings.logLineCommonFormatStr, sequenceNumber,
//				timeInSeconds, milliSec,
//				LogFormatStrings.dateFormatLong.format(now), mVehicleIdStr, event.toString()) + info);

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

	static public void closeLogFile(String reason) {
		if (activePrintWriter != null) {
			logEvent(LogEvents.CLOSING_FILE, reason);
			activePrintWriter.close();
			activePrintWriter = null;
		}
	}
}
