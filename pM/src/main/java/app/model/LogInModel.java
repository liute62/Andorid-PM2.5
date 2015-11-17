package app.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liuhaodong1 on 15/11/10.
 */
public class LogInModel extends BaseModel {

    public static final String STATUS = "status";

    public static final String ACCESS_TOKEN = "access_token";

    public static final String USERID = "userid";

    private String status;

    private String access_token;

    private String userid;

    public static LogInModel parse(JSONObject object) throws JSONException {
        LogInModel bean = new LogInModel();
        if (object.has(STATUS)) {
            bean.setStatus(object.getString(STATUS));
        }
        if (object.has(ACCESS_TOKEN)) {
            bean.setAccess_token(object.getString(ACCESS_TOKEN));
        }
        if (object.has(USERID)) {
            bean.setUserid(object.getString(USERID));
        }
        return bean;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }
}
