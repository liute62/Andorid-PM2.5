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
import app.utils.ACache;
import app.utils.Const;
import app.utils.HttpUtil;
import app.utils.VolleyQueue;

public class RegisterActivity extends Activity implements View.OnClickListener {

    EditText mInviteCode;
    EditText mUsername;
    EditText mPassword;
    EditText mConfirmPwd;
    EditText mLastname;
    EditText mFirstname;
    EditText mEmail;
    EditText mPhone;
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

    private void viewInitial() {
        mInviteCode = (EditText) findViewById(R.id.register_invite_code);
        mUsername = (EditText) findViewById(R.id.register_username);
        mPassword = (EditText) findViewById(R.id.register_password);
        mConfirmPwd = (EditText) findViewById(R.id.register_confirm_pwd);
        mMale = (CheckBox) findViewById(R.id.register_gender_male);
        mFemale = (CheckBox) findViewById(R.id.register_gender_female);
        mFirstname = (EditText) findViewById(R.id.register_firstname);
        mLastname = (EditText) findViewById(R.id.register_lastname);
        mEmail = (EditText) findViewById(R.id.register_mail);
        mSure = (TextView) findViewById(R.id.register_sure);
        mCancel = (TextView) findViewById(R.id.register_cancel);
        mPhone = (EditText) findViewById(R.id.register_phone);
    }

    private void listenerInitial() {
        mMale.setOnClickListener(this);
        mFemale.setOnClickListener(this);
        mSure.setOnClickListener(this);
        mCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.register_gender_male:
                if (mFemale.isChecked()) {
                    mFemale.setChecked(false);
                }
                break;
            case R.id.register_gender_female:
                if (mMale.isChecked()) {
                    mMale.setChecked(false);
                }
                break;
            case R.id.register_cancel:
                RegisterActivity.this.finish();
                break;
            case R.id.register_sure:
                //if input meet the requirement
                boolean inputSuccess = true;
                int gender = 0;
                String username = mUsername.getText().toString();
                String password = mPassword.getText().toString();
                String pwdconfirm = mConfirmPwd.getText().toString();
                String lastname = mLastname.getText().toString();
                String firstname = mFirstname.getText().toString();
                String mail = mEmail.getText().toString();
                String phone = mPhone.getText().toString();
                if (username == null || username.trim().equals("")) inputSuccess = false;
                if (password == null || password.trim().equals("")) inputSuccess = false;
                if (pwdconfirm == null || pwdconfirm.trim().equals("")) inputSuccess = false;
                if (lastname == null || lastname.trim().equals("")) inputSuccess = false;
                if (firstname == null || firstname.trim().equals("")) inputSuccess = false;
                if (mail == null || mail.trim().equals("")) inputSuccess = false;
                if (mMale.isChecked()) {
                    gender = 1;
                }
                if (mFemale.isChecked()) {
                    gender = 2;
                }
                if (gender == 0) inputSuccess = false;
                if (inputSuccess) {
                    if (!password.equals(pwdconfirm))
                        Toast.makeText(RegisterActivity.this, Const.Info_Register_pwdError, Toast.LENGTH_SHORT).show();
                    else {
                        UserModel userModel = new UserModel();
                        userModel.setName(username);
                        userModel.setFirstname(firstname);
                        userModel.setLastname(lastname);
                        userModel.setSex(String.valueOf(gender));
                        userModel.setEmail(mail);
                        userModel.setPassword(password);
                        userModel.setPhone(phone);
                        if (!isRegTaskRun) {
                            try {
                                register(userModel);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, Const.Info_Register_InputEmpty, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void register(final UserModel userModel) throws JSONException {
        isRegTaskRun = true;
        String url = HttpUtil.Register_url;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, userModel.toJsonObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isRegTaskRun = false;
                try {
                    String status = response.getString("status");
                    String access_token = response.getString("access_token");
                    String user_id = response.getString("userid");
                    if (status.equals("1")) {
                        Const.CURRENT_ACCESS_TOKEN = access_token;
                        Const.CURRENT_USER_NAME = userModel.getName();
                        Const.CURRENT_USER_NICKNAME = userModel.getLastname() + userModel.getFirstname();
                        Const.CURRENT_USER_GENDER = userModel.getSex();
                        ACache aCache = ACache.get(RegisterActivity.this);
                        aCache.put(Const.Cache_Access_Token, Const.CURRENT_ACCESS_TOKEN);
                        aCache.put(Const.Cache_User_Id, user_id);
                        aCache.put(Const.Cache_User_Name, Const.CURRENT_USER_NAME);
                        aCache.put(Const.Cache_User_Nickname, Const.CURRENT_USER_NICKNAME);
                        aCache.put(Const.Cache_User_Gender, Const.CURRENT_USER_GENDER);
                        RegisterActivity.this.setResult(1); // notify the profile fragment to update the login state
                        RegisterActivity.this.finish();
                        Toast.makeText(getApplicationContext(), Const.Info_Register_Success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), Const.Info_Register_Failed, Toast.LENGTH_SHORT).show();
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
