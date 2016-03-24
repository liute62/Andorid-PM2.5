package app.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.example.pm.R;
import com.umeng.socialize.bean.CallbackConfig;
import com.umeng.socialize.bean.CustomPlatform;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.bean.SocializeEntity;
import com.umeng.socialize.controller.UMServiceFactory;
import com.umeng.socialize.controller.UMSocialService;
import com.umeng.socialize.controller.listener.SocializeListeners;
import com.umeng.socialize.media.QQShareContent;
import com.umeng.socialize.media.QZoneShareContent;
import com.umeng.socialize.media.SinaShareContent;
import com.umeng.socialize.media.TencentWbShareContent;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.sso.QZoneSsoHandler;
import com.umeng.socialize.sso.SinaSsoHandler;
import com.umeng.socialize.sso.TencentWBSsoHandler;
import com.umeng.socialize.sso.UMQQSsoHandler;
import com.umeng.socialize.weixin.controller.UMWXHandler;
import com.umeng.socialize.weixin.media.CircleShareContent;
import com.umeng.socialize.weixin.media.WeiXinShareContent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Administrator on 12/10/2015.
 */
public class ShareUtils {

    public static final String TAG = "ShareUtils";
    UMSocialService mController;
    String shareContent = "shareContent";
    Activity mActivity;
    private String QQ_APP_ID = "100424468";
    private String QQ_APP_KEY = "c7394704798a158208a74ab60104f0ba";
    private String WEIXIN_APP_ID = "wx24d29d8146699834";
    private String WEIXIN_APP_SECRETE = "2dfbce45b493fde58d7075d7d2aed5fd";
    private String targetUrl = "http://www.bio3air.com/";
    private Bitmap mImage = null;

    public ShareUtils(Activity mActivity,UMSocialService mController){
        this.mController = mController;
        this.mActivity = mActivity;
        configPlatforms();
    }

    public void share(Bitmap image){
        mImage = image;
        mController.getConfig().removePlatform(SHARE_MEDIA.RENREN, SHARE_MEDIA.DOUBAN);
        mController.openShare(mActivity, false);
        setQQShareContent();
        setQQZoneShareContent();
        setSinaShareContent();
        setWXShareContent();
        setWXCircleShareContent();
        setTenWeiboContent();
    }

    private void configPlatforms(){
        addSinaPlatform();
        addQQQZonePlatform();
        addWXPlatform();
        addTenWeiboPlatform();
    }

//    private void testForSendingWechat(int type){
//        Intent intent = new Intent();
//        //ComponentName cmp = new ComponentName("com.tencent.mm","com.tencent.mm.ui.LauncherUI");
//        ComponentName cmp = null;
//        switch (type){
//            case 1:
//                cmp = new ComponentName("com.tencent.mm","com.tencent.mm.ui.tools.ShareImgUI");
//                break;
//            case 2:
//                cmp = new ComponentName("com.tencent.mm",
//                        "com.tencent.mm.ui.tools.ShareToTimeLineUI");
//                break;
//        }
//        intent.setAction(Intent.ACTION_SEND);
//        intent.setType("image/*");
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        intent.putExtra("kdescription", "test for intent of wechat");
//        Bitmap bitmap = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.card_avatar_bar);
//        File f = new File("/sdcard/namecard/", "test");
//        try {
//            FileOutputStream out = new FileOutputStream(f);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
//            out.flush();
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Uri uri = Uri.fromFile(f);
//        intent.putExtra(Intent.EXTRA_STREAM, uri);
//        intent.setComponent(cmp);
//        mActivity.startActivityForResult(intent, 0);
//    }

    private void setQQShareContent(){
        QQShareContent qqShareContent = new QQShareContent();
        qqShareContent.setShareContent(mActivity.getResources().getString(R.string.share_content));
        qqShareContent.setTitle(mActivity.getResources().getString(R.string.share_title));
        qqShareContent.setShareImage(new UMImage(mActivity, mImage));
        qqShareContent.setTargetUrl(targetUrl);
        mController.setShareMedia(qqShareContent);
    }

    private void setQQZoneShareContent(){
        QZoneShareContent qzone = new QZoneShareContent();
        qzone.setShareContent(mActivity.getResources().getString(R.string.share_content));
        qzone.setTitle(mActivity.getResources().getString(R.string.share_title));
        qzone.setShareImage(new UMImage(mActivity, mImage));
        qzone.setTargetUrl(targetUrl);
        mController.setShareMedia(qzone);
    }

    private void setSinaShareContent(){
        SinaShareContent sina = new SinaShareContent();
        sina.setShareContent(mActivity.getResources().getString(R.string.share_content));
        sina.setTitle(mActivity.getResources().getString(R.string.share_title));
        sina.setShareImage(new UMImage(mActivity, mImage));
        sina.setTargetUrl(targetUrl);
        mController.setShareMedia(sina);
    }

    private void setWXShareContent(){
        WeiXinShareContent weiXin = new WeiXinShareContent();
        weiXin.setShareContent(mActivity.getResources().getString(R.string.share_content));
        weiXin.setTitle(mActivity.getResources().getString(R.string.share_title));
        weiXin.setShareImage(new UMImage(mActivity, mImage));
        weiXin.setTargetUrl(targetUrl);
        mController.setShareMedia(weiXin);
    }

    private void setWXCircleShareContent(){
        CircleShareContent circle = new CircleShareContent();
        circle.setShareContent(mActivity.getResources().getString(R.string.share_content));
        circle.setTitle(mActivity.getResources().getString(R.string.share_title));
        circle.setShareImage(new UMImage(mActivity, mImage));
        circle.setTargetUrl(targetUrl);
        mController.setShareMedia(circle);
    }

    private void setTenWeiboContent(){
        TencentWbShareContent tenWB = new TencentWbShareContent();
        tenWB.setShareContent(mActivity.getResources().getString(R.string.share_content));
        tenWB.setTitle(mActivity.getResources().getString(R.string.share_title));
        tenWB.setShareImage(new UMImage(mActivity, mImage));
        tenWB.setTargetUrl(targetUrl);
        mController.setShareMedia(tenWB);
    }

    private void addSinaPlatform(){
        mController.getConfig().setSsoHandler(new SinaSsoHandler());
    }

    private void addQQQZonePlatform() {
        String appId = QQ_APP_ID;
        String appKey = QQ_APP_KEY;
        // 添加QQ支持, 并且设置QQ分享内容的target url
        UMQQSsoHandler qqSsoHandler = new UMQQSsoHandler(mActivity,
                appId, appKey);
        //qqSsoHandler.setTargetUrl("http://www.umeng.com/social");
        qqSsoHandler.addToSocialSDK();
        // 添加QZone平台
        QZoneSsoHandler qZoneSsoHandler = new QZoneSsoHandler(mActivity, appId, appKey);
        qZoneSsoHandler.addToSocialSDK();
    }

    private void addWXPlatform() {
        // 注意：在微信授权的时候，必须传递appSecret
        // wx967daebe835fbeac是你在微信开发平台注册应用的AppID, 这里需要替换成你注册的AppID
        String appId = WEIXIN_APP_ID;
        String appSecret = WEIXIN_APP_SECRETE;
        // 添加微信平台
        UMWXHandler wxHandler = new UMWXHandler(mActivity, appId, appSecret);
        wxHandler.addToSocialSDK();
        // 支持微信朋友圈
        UMWXHandler wxCircleHandler = new UMWXHandler(mActivity, appId, appSecret);
        wxCircleHandler.setToCircle(true);
        wxCircleHandler.addToSocialSDK();
    }

    private void addTenWeiboPlatform(){
        TencentWBSsoHandler tencentWBSsoHandler = new TencentWBSsoHandler();
        tencentWBSsoHandler.addToSocialSDK();
    }
}
