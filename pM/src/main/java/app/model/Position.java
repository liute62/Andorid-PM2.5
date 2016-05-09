package app.model;

public class Position {
    private String name;
    private double latitude;
    private double longitude;
    private String alias;

    public Position() {

    }

    public Position(String name, double latitude, double longtitude,
                    String alias) {
        super();
        this.name = name;
        this.latitude = latitude;
        this.longitude = longtitude;
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = this.longitude;
    }

    public String toString() {
        return name + ":" + latitude + "," + longitude;
    }

}
