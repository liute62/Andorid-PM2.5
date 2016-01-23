package app.view.widget;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.example.pm.R;

/**
 * Created by Administrator on 1/21/2016.
 */
public class DialogConfirm extends Dialog {

    Context mContext;
    TextView mTitle;
    TextView mContent;
    TextView mConfirm;
    TextView mCancel;
    String title = "title";
    String content = "content";
    String confirm = "CONFIRM";
    String cancel = "CANCEL";
    View.OnClickListener cancelListener = null;
    View.OnClickListener confirmListener = null;

    public DialogConfirm(Context context) {
        super(context);
        mContext = context;
    }

    public DialogConfirm(Context context,String title,String content){
        super(context);
        mContext = context;
        this.title = title;
        this.content = content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.widget_dialog_confirm);
        mTitle = (TextView)findViewById(R.id.dialog_confirm_title);
        mContent = (TextView)findViewById(R.id.dialog_confirm_content);
        mConfirm = (TextView)findViewById(R.id.dialog_confirm_confirm);
        mCancel = (TextView)findViewById(R.id.dialog_confirm_cancel);
        setTitle(title);
        setContent(content);
        setConfirmText(confirm);
        setCancelText(cancel);
        setConfirmListener(confirmListener);
        setCancelListener(cancelListener);
    }

    public void setTitle(String str){
        title = str;
        if(mTitle != null) mTitle.setText(title);
    }

    public void setContent(String str){
        content = str;
        if(mContent != null) mContent.setText(content);
    }

    public void setConfirmText(String str){
        confirm = str;
        if(mConfirm != null) mConfirm.setText(confirm);
    }

    public void setCancelText(String str){
        cancel = str;
        if(mCancel != null) mCancel.setText(cancel);
    }

    public void setConfirmListener(View.OnClickListener listener){
        confirmListener = listener;
        if(mConfirm != null && listener != null) mConfirm.setOnClickListener(listener);
    }

    public void setCancelListener(View.OnClickListener listener){
        cancelListener = listener;
        if(mCancel != null && listener != null) mCancel.setOnClickListener(listener);
    }

    public void setAllDismissListener(){
            cancelListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogConfirm.this.dismiss();
                }
            };
            confirmListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogConfirm.this.dismiss();
                }
            };
        if(mConfirm != null) mConfirm.setOnClickListener(confirmListener);
        if(mCancel != null) mCancel.setOnClickListener(cancelListener);
    }
}
