package com.example.pm;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import app.model.UserModel;
import app.utils.Const;
import app.utils.HttpUtil;
import app.utils.VolleyQueue;

public class RegisterActivity extends Activity implements View.OnClickListener{

    EditText mInviteCode;
    EditText mUsername;
    EditText mPassword;
    EditText mConfirmPwd;
    CheckBox mMale;
    CheckBox mFemale;
    TextView mSure;
    TextView mCancel;
    boolean isRegTaskRun = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        viewInitial();
        listenerInitial();
    }

    private void viewInitial(){
        mInviteCode = (EditText)findViewById(R.id.register_invite_code);
        mUsername = (EditText)findViewById(R.id.register_username);
        mPassword = (EditText)findViewById(R.id.register_password);
        mConfirmPwd  = (EditText)findViewById(R.id.register_confirm_pwd);
        mMale = (CheckBox)findViewById(R.id.register_gender_male);
        mFemale = (CheckBox)findViewById(R.id.register_gender_female);
        mSure = (TextView)findViewById(R.id.register_sure);
        mCancel = (TextView)findViewById(R.id.register_cancel);
    }

    private void listenerInitial(){
        mSure.setOnClickListener(this);
        mCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.register_cancel:
                RegisterActivity.this.finish();
                break;
            case R.id.register_sure:
                //if input meet the requirement
                if(true){
                    UserModel userModel = new UserModel();
                    userModel.setName(mUsername.getText().toString());
                    userModel.setFirstname(mUsername.getText().toString());
                    userModel.setLastname(mUsername.getText().toString());
                    userModel.setSex("1");
                    userModel.setEmail("test@sina.com");
                    userModel.setPassword(mPassword.getText().toString());
                    userModel.setPhone("12342141");
                    if(!isRegTaskRun) {
                        try {
                            register(userModel);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
        }
    }

    private void register(UserModel userModel) throws JSONException {
        isRegTaskRun = true;
        String url = HttpUtil.Register_url;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,userModel.toJsonObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isRegTaskRun = false;
                try {
                    String status = response.getString("status");
                    String access_token = response.getString("access_token");
                    String user_id = response.getString("userid");
                    if (status.equals("1")){
                        RegisterActivity.this.finish();
                        Toast.makeText(getApplicationContext(), Const.Info_Register_Success, Toast.LENGTH_LONG).show();
                    }else {
                        Toast.makeText(getApplicationContext(), Const.Info_Register_Failed, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("register resp", response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isRegTaskRun = false;
                Toast.makeText(getApplicationContext(), Const.ERROR_REGISTER_WRONG, Toast.LENGTH_SHORT).show();
            }

        });
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }
}
