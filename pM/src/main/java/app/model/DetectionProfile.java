package app.model;

/**
 * Created by Haodong on 3/22/2016.
 * This is a model for indoor and outdoor service detection.
 */
public class DetectionProfile {

    private double confidence;
    private String environment;

    public DetectionProfile(String env){
        environment = env;
        confidence = 0.0;
    }

    public double getConfidence() {
        if(Double.isNaN(confidence)) {
            return 0.0;
        }
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
