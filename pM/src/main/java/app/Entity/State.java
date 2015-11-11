package app.Entity;

import app.utils.DBConstants;
import nl.qbusict.cupboard.annotation.Column;

/**
 * Created by sweet on 15-10-4.
 */
public class State {
	@Column(DBConstants.DB_MetaData.STATE_ID_COL)
    private Long id;
	@Column(DBConstants.DB_MetaData.STATE_USER_ID_COL)
	private String userid;
	@Column(DBConstants.DB_MetaData.STATE_TIME_POINT_COL)
	private String time_point;
	@Column(DBConstants.DB_MetaData.STATE_LONGTITUDE_COL)
	private String longtitude;
	@Column(DBConstants.DB_MetaData.STATE_LATITUDE_COL)
	private String latitude;
	@Column(DBConstants.DB_MetaData.STATE_OUTDOOR_COL)
	private String outdoor;
	@Column(DBConstants.DB_MetaData.STATE_STATUS_COL)
	private String status;
	@Column(DBConstants.DB_MetaData.STATE_STEPS_COL)
	private String steps;
	@Column(DBConstants.DB_MetaData.STATE_AVG_RATE_COL)
	private String avg_rate;
	@Column(DBConstants.DB_MetaData.STATE_VENTILATION_VOLUME_COL)
	private String ventilation_volume;
	@Column(DBConstants.DB_MetaData.STATE_DENSITY_COL)
	private String density;
	@Column(DBConstants.DB_MetaData.STATE_PM25_COL)
	private String pm25;
	@Column(DBConstants.DB_MetaData.STATE_SOURCE_COL)
	private String source;

	public State() {

	}

	public State(String userid, String time_point, String longtitude, String latitude, String outdoor, String status, String steps, String avg_rate, String ventilation_volume, String density, String pm25, String source) {
		this.userid = userid;
		this.time_point = time_point;
		this.longtitude = longtitude;
		this.latitude = latitude;
		this.outdoor = outdoor;
		this.status = status;
		this.steps = steps;
		this.avg_rate = avg_rate;
		this.ventilation_volume = ventilation_volume;
        this.density = density;
		this.pm25 = pm25;
		this.source = source;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getTime_point() {
		return time_point;
	}

	public void setTime_point(String time_point) {
		this.time_point = time_point;
	}

	public String getLongtitude() {
		return longtitude;
	}

	public void setLongtitude(String longtitude) {
		this.longtitude = longtitude;
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

	public String getAvg_rate() {
		return avg_rate;
	}

	public void setAvg_rate(String avg_rate) {
		this.avg_rate = avg_rate;
	}

	public String getVentilation_volume() {
		return ventilation_volume;
	}

	public void setVentilation_volume(String ventilation_volume) {
		this.ventilation_volume = ventilation_volume;
	}

    public String getDensity() {
        return density;
    }

    public void setDensity(String density) {
        this.density = density;
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
