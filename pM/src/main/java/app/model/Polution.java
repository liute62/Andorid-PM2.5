package app.model;

public class Polution {
	private int id;
	private int aqi;
	private String area;
	private String position_name;
	private String station_code;
	private int PM2_5;
	private int PM2_5_24h;
	private String primary_pollutant;
	private String quality;
	private String time_point;
	
	
	public Polution(int id, int aqi, String area, String position_name,
			String station_code, int pM2_5, int pM2_5_24h,
			String primary_pollutant, String quality, String time_point) {
		super();
		this.id = id;
		this.aqi = aqi;
		this.area = area;
		this.position_name = position_name;
		this.station_code = station_code;
		PM2_5 = pM2_5;
		PM2_5_24h = pM2_5_24h;
		this.primary_pollutant = primary_pollutant;
		this.quality = quality;
		this.time_point = time_point;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getAqi() {
		return aqi;
	}
	public void setAqi(int aqi) {
		this.aqi = aqi;
	}
	public String getArea() {
		return area;
	}
	public void setArea(String area) {
		this.area = area;
	}
	public String getPosition_name() {
		return position_name;
	}
	public void setPosition_name(String position_name) {
		this.position_name = position_name;
	}
	public String getStation_code() {
		return station_code;
	}
	public void setStation_code(String station_code) {
		this.station_code = station_code;
	}
	public int getPM2_5() {
		return PM2_5;
	}
	public void setPM2_5(int pM2_5) {
		PM2_5 = pM2_5;
	}
	public int getPM2_5_24h() {
		return PM2_5_24h;
	}
	public void setPM2_5_24h(int pM2_5_24h) {
		PM2_5_24h = pM2_5_24h;
	}
	public String getPrimary_pollutant() {
		return primary_pollutant;
	}
	public void setPrimary_pollutant(String primary_pollutant) {
		this.primary_pollutant = primary_pollutant;
	}
	public String getQuality() {
		return quality;
	}
	public void setQuality(String quality) {
		this.quality = quality;
	}
	public String getTime_point() {
		return time_point;
	}
	public void setTime_point(String time_point) {
		this.time_point = time_point;
	}	
	
	public String toString() {
		String str = "";
		str += id + "," + id + "," + "," + aqi + "," + area + "," + "," + position_name
		+ "," + station_code + "," + PM2_5 + "," + PM2_5_24h + "," + primary_pollutant +
		"," + quality + "," + time_point;
		return str;
	}
}
