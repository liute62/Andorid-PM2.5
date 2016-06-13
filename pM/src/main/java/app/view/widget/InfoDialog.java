package app.view.widget;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.example.pm.R;

/**
 * Created by liuhaodong1 on 15/11/12.
 */
public class InfoDialog extends Dialog {

    private Button mSure;
    private Button mCancel;
    private TextView mContent;

    private String content;

    private View.OnClickListener cancelListener;
    private View.OnClickListener sureListener;

    public InfoDialog(Activity mActivity) {
        super(mActivity);
        setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.widget_dialog_info);
        mSure = (Button) findViewById(R.id.dialog_info_sure);
        mCancel = (Button) findViewById(R.id.dialog_info_cancel);
        mContent = (TextView) findViewById(R.id.dialog_info_content);
        if (content != null) {
            mContent.setText(content);
        }
        if (cancelListener != null) {
            mCancel.setOnClickListener(cancelListener);
        }
        if (sureListener != null) {
            mSure.setOnClickListener(sureListener);
        }
    }

    public void setContent(String text) {
        content = text;
    }

    public void setSureClickListener(View.OnClickListener listener) {
        sureListener = listener;
    }

    public void setCancelClickListener(View.OnClickListener listener) {
        cancelListener = listener;
    }

}
