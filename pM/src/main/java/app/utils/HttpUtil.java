package app.utils;

import android.app.Activity;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import app.model.HttpResult;
import app.model.UploadModel;
import app.model.UserModel;

/**
 * Created by liuhaodong1 on 15/11/10.
 */
public class HttpUtil {

    private static final String base_url = "http://ilab.tongji.edu.cn/pm25/web/restful/";

    public static final String Search_PM_url = base_url + "urban-airs/search";

    public static final String Register_url = base_url + "users/logon";

    public static final String Login_url = base_url + "users/login";

    public static final String Upload_url = base_url + "mobile-data/create";

    public static final String Modify_Pwd_url = base_url + "users/updatepassword";

    public static final String Reset_Pwd_url = base_url + "users/resetpassword?name=";

    public static final String ReadData_url = base_url + "mobile-datas";

    public static final String DeviceData_url = base_url + "device-datas";

    public static final String SearchCity_url = "http://api.map.baidu.com/geocoder?output=json";

    public static final String MonitorLocation_url = base_url + "area-positions";

    public static final String UploadBatch_url = base_url + "mobile-data/upload";

    private static HttpUtil instance;

    public static HttpUtil Instance() {
        if (instance == null) {
            instance = new HttpUtil();
        }
        return instance;
    }

//    /**
//     * Api-1
//     * @param mActivity
//     * @return
//     */
//    public static HttpResult SearchPMRequest(final Activity mActivity,String longitude,String latitude){
//        String url = HttpUtil.Search_PM_url;
//        url = url+"?longitude="+longitude+"&latitude="+latitude;
//        final HttpResult result = new HttpResult();
//        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject response) {
//                result.setResultBody(response.toString());
//                Toast.makeText(mActivity.getApplicationContext(), "Data Get Success!", Toast.LENGTH_LONG).show();
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                result.setResultBody(null);
//                Toast.makeText(mActivity.getApplicationContext(), "Data Get Fail!", Toast.LENGTH_SHORT).show();
//            }
//
//        });
//        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
//        return result;
//    }

    /**
     * Api-1.2
     *
     * @param mActivity
     * @return
     */
    public static HttpResult SearchPMRequest(final Activity mActivity, String longitude, String latitude, String time_point) {
        String url = HttpUtil.Search_PM_url;
        url = url + "?longitude=" + longitude + "&latitude=" + latitude + "&time_point=" + time_point;
        final HttpResult result = new HttpResult();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                result.setResultBody(response.toString());
                Toast.makeText(mActivity.getApplicationContext(), "Data Get Success!", Toast.LENGTH_LONG).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                result.setResultBody(null);
                Toast.makeText(mActivity.getApplicationContext(), "Data Get Fail!", Toast.LENGTH_SHORT).show();
            }

        });
        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        return result;
    }

    /**
     * Api-2
     *
     * @param mActivity
     * @param model
     * @return
     * @throws JSONException
     */
    public static HttpResult logOnRequest(final Activity mActivity, UserModel model) throws JSONException {
        String url = HttpUtil.Register_url;
        final HttpResult result = new HttpResult();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, model.toJsonObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                result.setResultBody(response.toString());
                Toast.makeText(mActivity.getApplicationContext(), "Register Success!", Toast.LENGTH_LONG).show();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                result.setResultBody(null);
                Toast.makeText(mActivity.getApplicationContext(), "Register Failed!", Toast.LENGTH_SHORT).show();
            }
        });
        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        return result;
    }

//    /**
//     * Api-3
//     * @param mActivity
//     * @param name
//     * @param password
//     * @return
//     * @throws JSONException
//     */
//    private HttpResult logInRequestResult;
//    public HttpResult logInRequest(final Activity mActivity,String name,String password) throws JSONException {
//        String url = HttpUtil.Login_url;
//        JSONObject object = new JSONObject();
//        object.put("name",name);
//        object.put("password",password);
//        logInRequestResult = new HttpResult();
//        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, object, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject response) {
//                logInRequestResult.setIsSuccess(true);
//                logInRequestResult.setResultBody(response.toString());
//                Log.e("sea2",logInRequestResult.getResultBody().toString());
//                Toast.makeText(mActivity.getApplicationContext(), "Login Success!", Toast.LENGTH_SHORT).show();
//
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                logInRequestResult.setIsSuccess(false);
//                logInRequestResult.setResultBody(null);
//                Toast.makeText(mActivity.getApplicationContext(), "Login Failed!", Toast.LENGTH_SHORT).show();
//
//            }
//        });
//        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
//    }

    /**
     * Api-4
     *
     * @throws JSONException
     */
    public static HttpResult UploadRequest(final Activity mActivity, UploadModel model) throws JSONException {
        String url = HttpUtil.Upload_url;
        final HttpResult result = new HttpResult();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, model.toJsonObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                result.setResultBody(response.toString());
                Toast.makeText(mActivity.getApplicationContext(), "Upload Success!", Toast.LENGTH_SHORT).show();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                result.setResultBody(null);
                Toast.makeText(mActivity.getApplicationContext(), "Upload Fail!", Toast.LENGTH_SHORT).show();
            }
        });

        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        return result;
    }

    /**
     * Api-5
     *
     * @param mActivity
     * @param userid
     * @param time_point
     * @return
     */
    public static HttpResult ReadData(final Activity mActivity, String userid, String time_point) {
        String url = HttpUtil.ReadData_url;
        url = url + "?userid=" + userid + "&time_point=" + time_point;
        final HttpResult result = new HttpResult();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                result.setResultBody(response.toString());
                Toast.makeText(mActivity.getApplicationContext(), "Get Data Info Success!", Toast.LENGTH_SHORT).show();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                result.setResultBody(null);
                Toast.makeText(mActivity.getApplicationContext(), "Get Data Info Failed!", Toast.LENGTH_SHORT).show();
            }
        });

        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        return result;
    }

    /**
     * Api-6
     *
     * @param mActivity
     * @param devid
     * @param time_point
     * @return
     */
    public static HttpResult GetDevice(final Activity mActivity, String devid, String time_point) {
        String url = HttpUtil.DeviceData_url;
        url = url + "?devid=" + devid + "&time_point=" + time_point;
        final HttpResult result = new HttpResult();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                result.setResultBody(response.toString());
                Toast.makeText(mActivity.getApplicationContext(), "Get Device Info Success!", Toast.LENGTH_SHORT).show();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                result.setResultBody(null);
                Toast.makeText(mActivity.getApplicationContext(), "Get Device Info Failed!", Toast.LENGTH_SHORT).show();
            }
        });

        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        return result;
    }

}
