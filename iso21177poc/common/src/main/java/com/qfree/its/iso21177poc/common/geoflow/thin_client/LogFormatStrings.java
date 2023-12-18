package com.qfree.its.iso21177poc.common.geoflow.thin_client;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class LogFormatStrings {
	//Date formats
	public static final DateTimeFormatter  dateFormat;
	public static final DateTimeFormatter  dateFormatShortDate;
	public static final DateTimeFormatter  dateFormatShortTime;
	public static final DateTimeFormatter  dateFormatShorterTime;
	public static final DateTimeFormatter  obs__dateFormatStd;
	public static final DateTimeFormatter dateFormatLong;

	static {
		dateFormat = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
		dateFormatShortDate = DateTimeFormatter.ofPattern("yyyy_MM_dd");
		dateFormatShortTime = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
		dateFormatShorterTime = DateTimeFormatter.ofPattern("HH:mm:ss");
		obs__dateFormatStd = DateTimeFormatter.ofPattern("MMM dd',' yyyy HH:mm:ss");
		dateFormatLong = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	}

	//Csv log line formats
	public static final String logLineCommonFormatStr =
			/*Sequence number*/						"%d;" +
			/*Linux time*/							"%d.%03d;" +
			/*Timestamp som string fra linux*/		"%s;" +
			/*Identifier*/							"%s;" +
			/*Event/GPS/HMI*/						"%s;";
	
	public static final String logLineGPSFormatStr =
			/*Longitude*/		"%.6f;" + 
			/*Latitude*/		"%.6f;" +
			/*Altitude*/		"%.2f;" +
			/*Bearing*/			"%.2f;" +
			/*Speed*/			"%.2f;" +
			/*nSats*/			"%d;" +
			/*flag*/			"%d;" +
			/*hDop*/			"%.2f;" +
			/*integrity*/		"%.2f;";

	public static final String logLineDefaultFormatStr =
			/*info*/			"%s;";
}
