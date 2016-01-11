package app.view.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.pm.R;

import app.utils.Const;
import app.utils.ShortcutUtil;

/**
 * Created by Administrator on 1/11/2016.
 */
public class DialogInputWeight extends Dialog implements View.OnClickListener{

    EditText mContent;
    Button mConfirm;
    Button mCancel;
    Context mContext;
    Handler mHandler;

    public DialogInputWeight(Context context,Handler mParent) {
        super(context);
        this.mContext = context;
        this.mHandler = mParent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_dialog_input_weight);
        mConfirm = (Button)findViewById(R.id.input_weight_confirm);
        mConfirm.setOnClickListener(this);
        mCancel = (Button)findViewById(R.id.input_weight_cancel);
        mCancel.setOnClickListener(this);
        mContent = (EditText)findViewById(R.id.input_weight_content);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.input_weight_cancel:
                DialogInputWeight.this.dismiss();
                break;
            case R.id.input_weight_confirm:
                String content = mContent.getText().toString();
                if(ShortcutUtil.isWeightInputCorrect(content)){
                    Message message = new Message();
                    message.what = Const.Handler_Input_Weight;
                    message.obj = content;
                    mHandler.sendMessage(message);
                }else {
                    Toast.makeText(mContext.getApplicationContext(), Const.Info_Input_Weight_Error,Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
