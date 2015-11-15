package app.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liuhaodong1 on 15/11/11.
 */
public class PMModel {

    public static final String PM25 = "PM25";

    public static final String PM10 = "PM10";

    public static final String STATUS = "status";

    public static final String TEMPERATURE = "Temperature";

    public static final String HUMIDITY = "Humidity";

    public static final String UPDATETIME = "UpdateTime";

    public static final String ZEROTHREE = "03";

    public static final String AQI = "AQI";

    public static final String NAME = "Name";

    public static final String SO2 = "SO2";

    public static final String NO2 = "NO2";

    public static final String CO = "CO";

    public static final String PRESSURE = "Pressure";

    public static final String WIND = "Wind";

    private String pm25;

    private String pm10;

    private String status;

    private String temperature;

    private String humidity;

    private String update_time;

    private String zero_three;

    private String aqi;

    private String name;

    private String so2;

    private String no2;

    private String co;

    private String pressure;

    private String wind;

    private String pm_breath_hour;

    private String pm_breath_today;

    private String pm_breath_week;

    public PMModel(){
        pm25 = "0";
        pm10 = "0";
        status = "0";
        temperature = "0";
        humidity = "0";
        update_time = "0";
        zero_three = "0";
        aqi = "0";
        name = "0";
        so2 = "0";
        no2 = "0";
        co = "0";
        pressure = "0";
        wind = "0";
        pm_breath_hour = "0";
        pm_breath_today = "0";
        pm_breath_week = "0";
    }

    public static PMModel parse(JSONObject object) throws JSONException {
        PMModel bean = new PMModel();
        if (object.has(PM25)) {
            bean.setPm25(object.getString(PM25));
        }if(object.has(PM10)){
            bean.setPm10(object.getString(PM10));
        }if(object.has(STATUS)){
            bean.setStatus(object.getString(STATUS));
        }if(object.has(TEMPERATURE)){
            bean.setTemperature(object.getString(TEMPERATURE));
        }if(object.has(HUMIDITY)){
            bean.setHumidity(object.getString(HUMIDITY));
        }if(object.has(UPDATETIME)){
            bean.setUpdate_time(object.getString(UPDATETIME));
        }if(object.has(ZEROTHREE)){
            bean.setZero_three(object.getString(ZEROTHREE));
        }if(object.has(AQI)){
            bean.setAqi(object.getString(AQI));
        }if(object.has(NAME)){
            bean.setName(object.getString(NAME));
        }if(object.has(SO2)){
            bean.setSo2(object.getString(SO2));
        }if(object.has(NO2)){
            bean.setNo2(object.getString(NO2));
        }if(object.has(CO)){
            bean.setCo(object.getString(CO));
        }if(object.has(PRESSURE)){
            bean.setPressure(object.getString(PRESSURE));
        }if(object.has(WIND)){
            bean.setWind(object.getString(WIND));
        }
        return bean;
    }

    public String getPm25() {
        return pm25;
    }

    public void setPm25(String pm25) {
        this.pm25 = pm25;
    }

    public String getPm10() {
        return pm10;
    }

    public void setPm10(String pm10) {
        this.pm10 = pm10;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(String update_time) {
        this.update_time = update_time;
    }

    public String getZero_three() {
        return zero_three;
    }

    public void setZero_three(String zero_three) {
        this.zero_three = zero_three;
    }

    public String getAqi() {
        return aqi;
    }

    public void setAqi(String aqi) {
        this.aqi = aqi;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSo2() {
        return so2;
    }

    public void setSo2(String so2) {
        this.so2 = so2;
    }

    public String getNo2() {
        return no2;
    }

    public void setNo2(String no2) {
        this.no2 = no2;
    }

    public String getCo() {
        return co;
    }

    public void setCo(String co) {
        this.co = co;
    }

    public String getPressure() {
        return pressure;
    }

    public void setPressure(String pressure) {
        this.pressure = pressure;
    }

    public String getWind() {
        return wind;
    }

    public void setWind(String wind) {
        this.wind = wind;
    }

    public String getPm_breath_hour() {
        return pm_breath_hour;
    }

    public void setPm_breath_hour(String pm_breath_hour) {
        this.pm_breath_hour = pm_breath_hour;
    }

    public String getPm_breath_today() {
        return pm_breath_today;
    }

    public void setPm_breath_today(String pm_breath_toady) {
        this.pm_breath_today = pm_breath_toady;
    }

    public String getPm_breath_week() {
        return pm_breath_week;
    }

    public void setPm_breath_week(String pm_breath_week) {
        this.pm_breath_week = pm_breath_week;
    }
}
