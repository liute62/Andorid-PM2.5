package app.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liuhaodong1 on 15/11/9.
 * @deprecated
 */
public class HttpResult extends BaseModel {

    private String resultBody;

    private Boolean isSuccess;

    public String getResultBody() {
        return resultBody;
    }

    public void setResultBody(String resultBody) {
        this.resultBody = resultBody;
    }

    public Boolean getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(resultBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public LogInModel toLogInModel() {
        try {
            if (isSuccess == true)
                return LogInModel.parse(new JSONObject(resultBody));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
