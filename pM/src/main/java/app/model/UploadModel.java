package app.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liuhaodong1 on 15/11/10.
 */
public class UploadModel extends BaseModel{

    public static final String USERID = "userid";

    public static final String TIME_POINT = "time_point";

    public static final String LONGITUDE = "longitude";

    public static final String LATITUDE = "latitude";

    public static final String OUTDOOR = "outdoor";

    public static final String STATUS = "status";

    public static final String STEPS = "steps";

    public static final String AVG_RATE = "avg_rate";

    public static final String VENTILATION_VOLUME = "ventilation_volume";

    public static final String PM25 = "pm25";

    public static final String SOURCE = "source";

    private String userID;

    private String timePoint;

    private String longitude;

    private String latitude;

    private String outdoor;

    private String status;

    private String steps;

    private String avgRate;

    private String ventilationVolume;

    private String pm25;

    private String source;

    public static UploadModel AddDataForTest(){
        UploadModel model = new UploadModel();
        model.setUserID("55");
        model.setTimePoint("2013-08-20 12:00:00");
        model.setLongitude("123.567891");
        model.setLatitude("123.567891");
        model.setOutdoor("1");
        model.setStatus("1");
        model.setSteps("12");
        model.setAvgRate("12");
        model.setVentilationVolume("12");
        model.setPm25("12");
        model.setSource("2");
        return model;
    }

    public static UploadModel parse(JSONObject object) throws JSONException {
        UploadModel bean = new UploadModel();
        if (object.has(USERID)) {
            bean.setUserID(object.getString(USERID));
        }
        if (object.has(TIME_POINT)) {
            bean.setTimePoint(object.getString(TIME_POINT));
        }
        if (object.has(LONGITUDE)) {
            bean.setLongitude(object.getString(LONGITUDE));
        }
        if (object.has(LATITUDE)) {
            bean.setLatitude(object.getString(LATITUDE));
        }
        if (object.has(OUTDOOR)) {
            bean.setOutdoor(object.getString(OUTDOOR));
        }
        if (object.has(STATUS)) {
            bean.setStatus(object.getString(STATUS));
        }
        if (object.has(STEPS)) {
            bean.setSteps(object.getString(STEPS));
        } if (object.has(AVG_RATE)) {
            bean.setAvgRate(object.getString(AVG_RATE));
        }
        if (object.has(VENTILATION_VOLUME)) {
            bean.setVentilationVolume(object.getString(VENTILATION_VOLUME));
        } if (object.has(PM25)) {
            bean.setPm25(object.getString(PM25));
        } if (object.has(SOURCE)) {
            bean.setSource(object.getString(SOURCE));
        }
        return bean;
    }

    public JSONObject toJsonObject() throws JSONException{
        JSONObject object = new JSONObject();
        if (userID != null) {
            object.put(USERID, getUserID());
        }if (timePoint != null) {
            object.put(TIME_POINT, getTimePoint());
        }if (longitude != null) {
            object.put(LONGITUDE, getLongitude());
        }if (latitude != null) {
            object.put(LATITUDE, getLatitude());
        }if (outdoor != null) {
            object.put(OUTDOOR, getOutdoor());
        }if (status != null){
            object.put(STATUS,getStatus());
        }if (steps != null){
            object.put(STEPS,getSteps());
        }if (avgRate != null){
            object.put(AVG_RATE,getAvgRate());
        }if (ventilationVolume != null){
            object.put(VENTILATION_VOLUME,getVentilationVolume());
        }if (pm25 != null){
            object.put(PM25,getPm25());
        }if (source != null){
            object.put(SOURCE,getSource());
        }
        return object;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getTimePoint() {
        return timePoint;
    }

    public void setTimePoint(String timePoint) {
        this.timePoint = timePoint;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getOutdoor() {
        return outdoor;
    }

    public void setOutdoor(String outdoor) {
        this.outdoor = outdoor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getAvgRate() {
        return avgRate;
    }

    public void setAvgRate(String avgRate) {
        this.avgRate = avgRate;
    }

    public String getVentilationVolume() {
        return ventilationVolume;
    }

    public void setVentilationVolume(String ventilationVolume) {
        this.ventilationVolume = ventilationVolume;
    }

    public String getPm25() {
        return pm25;
    }

    public void setPm25(String pm25) {
        this.pm25 = pm25;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
