package app.view.widget;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.pm.R;

import org.json.JSONException;
import org.json.JSONObject;

import app.utils.ACache;
import app.utils.Const;
import app.utils.HttpUtil;
import app.utils.VolleyQueue;

/**
 *
 */
public class ModifyPwdDialog extends Dialog implements OnClickListener {

    Activity mActivity;
    Button mSure;
    Button mBack;
    EditText mUser;
    EditText mPass;
    String username;
    String password;
    LoadingDialog mLoadingDialog;
    boolean flag;
    ACache aCache;
    Handler parentHandler;

    public ModifyPwdDialog(Activity mActivity) {
        super(mActivity);
        this.mActivity = mActivity;
        flag = true;
        aCache = ACache.get(mActivity);
    }

    public ModifyPwdDialog(Activity mActivity, Handler parent) {
        super(mActivity);
        this.mActivity = mActivity;
        flag = true;
        aCache = ACache.get(mActivity);
        parentHandler = parent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.widget_dialog_modify_pwd);
        mLoadingDialog = new LoadingDialog(mActivity);
        mSure = (Button) findViewById(R.id.dialog_modifypwd_sure);
        mBack = (Button) findViewById(R.id.dialog_modifypwd_cancel);
        mUser = (EditText) findViewById(R.id.dialog_modifypwd_user);
        mPass = (EditText) findViewById(R.id.dialog_modifypwd_pwd);
        mSure.setOnClickListener(this);
        mBack.setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.dialog_modifypwd_sure:
                username = mUser.getText().toString();
                password = mPass.getText().toString();
                int result = 3;//ShortcutUtil.judgeInput(username, password);
                if (result == 3) {
                    mLoadingDialog.show();
                    if (flag) {
                        try {
                            modify(username, password);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (result == 0) {
                    Toast.makeText(mActivity.getApplicationContext(), Const.Info_Login_Empty, Toast.LENGTH_SHORT).show();
                }
                if (result == 1) {
                    Toast.makeText(mActivity.getApplicationContext(), Const.Info_Login_Short, Toast.LENGTH_SHORT).show();
                }
                if (result == 2) {
                    Toast.makeText(mActivity.getApplicationContext(), Const.Info_Login_Space, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.dialog_info_cancel:
                this.dismiss();
                break;
            default:
                break;
        }
    }

    private void modify(String name, String password) throws JSONException {
        String url = HttpUtil.Modify_Pwd_url;
        JSONObject object = new JSONObject();
        object.put("access_token", Const.CURRENT_ACCESS_TOKEN);
        object.put("name", name);
        object.put("password", password);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, object, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                mLoadingDialog.dismiss();
                try {
                    String status = response.getString("status");
                    if (status.equals("1")) {
                        Toast.makeText(mActivity.getApplicationContext(), Const.Info_Modify_Pwd_Success, Toast.LENGTH_SHORT).show();
                        Const.CURRENT_ACCESS_TOKEN = response.getString("access_token");
                        aCache.put(Const.Cache_Access_Token, Const.CURRENT_ACCESS_TOKEN);
                        if (parentHandler != null) {
                            parentHandler.sendEmptyMessage(Const.Handler_Modify_Pwd_Success);
                        }
                        ModifyPwdDialog.this.dismiss();
                    } else {
                        mLoadingDialog.dismiss();
                        Toast.makeText(mActivity.getApplicationContext(), Const.Info_Modify_Pwd_Error, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mLoadingDialog.dismiss();
                Toast.makeText(mActivity.getApplicationContext(), Const.Info_No_Network, Toast.LENGTH_SHORT).show();

            }
        });
        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }
}
