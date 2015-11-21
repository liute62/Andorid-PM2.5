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

    public static final String LASTNAME = "lastname";

    public static final String FIRSTNAME = "firstname";

    public static final String GENDER = "sex";

    private String status;

    private String access_token;

    private String userid;

    private String lastname;

    private String firstname;

    private String gender;

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
        }if (object.has(LASTNAME)){
            bean.setLastname(object.getString(LASTNAME));
        }if (object.has(FIRSTNAME)){
            bean.setFirstname(object.getString(FIRSTNAME));
        }if (object.has(GENDER)){
            bean.setGender(object.getString(GENDER));
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

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
