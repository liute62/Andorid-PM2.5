package app.view.widget;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.example.pm.R;

import java.util.jar.Attributes;

/**
 * Created by Administrator on 1/11/2016.
 */
public class DialogNotification extends Dialog {

    Button mBack;

    public DialogNotification(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.widget_dialog_notification);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = RelativeLayout.LayoutParams.FILL_PARENT;
        params.width = RelativeLayout.LayoutParams.FILL_PARENT;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        mBack = (Button)findViewById(R.id.dialog_notification_back);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogNotification.this.dismiss();
            }
        });
    }
}
