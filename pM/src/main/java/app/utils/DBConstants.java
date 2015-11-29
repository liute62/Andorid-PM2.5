package app.utils;

public class DBConstants {
    public static final String DB_NAME = "Information.db";
    public static final String TABLE_NAME = "state";
    public static final int DB_VERSION = 1;

    public static class DB_MetaData {
        public static final String STATE_ID_COL = "_id";
        public static final String STATE_USER_ID_COL = "userid";
        public static final String STATE_TIME_POINT_COL = "time_point";
        public static final String STATE_LONGTITUDE_COL = "longtitude";
        public static final String STATE_LATITUDE_COL = "latitude";
        public static final String STATE_OUTDOOR_COL = "outdoor";
        public static final String STATE_STATUS_COL = "status";
        public static final String STATE_STEPS_COL = "steps";
        public static final String STATE_AVG_RATE_COL = "avg_rate";
        public static final String STATE_VENTILATION_VOLUME_COL = "ventilation_volume";
        public static final String STATE_DENSITY_COL = "density";
        public static final String STATE_PM25_COL = "pm25";
        public static final String STATE_SOURCE_COL = "source";
        public static final String STATE_HAS_UPLOAD = "upload";
        public static final String DEFAULT_ORDER = "_id desc";
    }

}
