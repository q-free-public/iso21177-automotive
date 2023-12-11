package com.qfree.its.iso21177poc.common.geoflow;

public class Config {
	//Local dirs, save to app internal storage, no need for permission. In principle secure location
	public static final String USERDATA_DIR = "userdata";
	public static final String LOG_DIR = "geoflow_thin_client_log";

	//LOCALHOST_URL are for local desktop testing, network_security_config.xml must be
	//included in manifest application tag for sending data over http (cleartext traffic)
	public static final String SERVER_URL = "https://geoflow.q-free.com";
	public static final String SERVLET_FILEPATH_DOWNLOAD_TOLLING_ZONES = "/geoflow/getzonerules";
	public static final String SERVLET_FILEPATH_UPLOAD_LOG_FILE = "/geoflow/ul/upload_log_file";
	public static final String SERVLET_FILEPATH_USER_UPDATE = "/geoflow/userdbupdate";
	
	// EventAdmin event topic
	public final static String FILE_UPLOAD_EVENT_TOPIC = "com/qfree/its/geoflow/thin/fileUploadEvent";
	public final static String HTTP_INVOICE_UPLOAD_EVENT = "com/qfree/its/geoflow/thick/fileUploadEvent";
	public final static String HTTP_USER_UPLOAD_EVENT = "com/qfree/its/geoflow/thick/userUploadEvent";
	public final static String HTTP_VEHICLE_UPLOAD_EVENT = "com/qfree/its/geoflow/thick/vehicleUploadEvent";

	// Tolling props
    public static final String INVOICE_INTERVAL = "1d";
}
