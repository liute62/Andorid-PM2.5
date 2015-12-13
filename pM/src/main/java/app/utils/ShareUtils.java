package app.utils;

import android.app.Activity;

import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.controller.UMServiceFactory;
import com.umeng.socialize.controller.UMSocialService;
import com.umeng.socialize.media.QZoneShareContent;
import com.umeng.socialize.sso.QZoneSsoHandler;
import com.umeng.socialize.sso.SinaSsoHandler;
import com.umeng.socialize.sso.UMQQSsoHandler;
import com.umeng.socialize.weixin.controller.UMWXHandler;
import com.umeng.socialize.weixin.media.CircleShareContent;

/**
 * Created by Administrator on 12/10/2015.
 */
public class ShareUtils {

    String shareContent = "shareContent";
    Activity mActivity;
    private String QQ_APP_ID = "100424468";
    private String QQ_APP_KEY = "c7394704798a158208a74ab60104f0ba";
    private String WEIXIN_APP_ID = "100424468";
    private String WEIXIN_APP_SECRETE = "c7394704798a158208a74ab60104f0ba";

    private final UMSocialService mController = UMServiceFactory
            .getUMSocialService("com.umeng.share");

    public ShareUtils(Activity mActivity){
        this.mActivity = mActivity;
        configPlatforms();
    }

    private void configPlatforms(){
        // 添加新浪SSO授权
        mController.getConfig().setSsoHandler(new SinaSsoHandler());
        // 添加QQ、QZone平台
        addQQQZonePlatform();
        // 添加微信、微信朋友圈平台
        addWXPlatform();
    }

    private void addCustomPlatforms() {
       // 添加微信平台
        addWXPlatform();
    }

    public void share(){
        mController.getConfig().removePlatform( SHARE_MEDIA.RENREN, SHARE_MEDIA.DOUBAN);
        mController.openShare(mActivity,false);
    }

    private void setShareContent() {
        mController.getConfig().setSsoHandler(new SinaSsoHandler());
        //参数1为当前Activity，参数2为开发者在QQ互联申请的APP ID，参数3为开发者在QQ互联申请的APP kEY.
        QZoneSsoHandler qZoneSsoHandler = new QZoneSsoHandler(mActivity,
                QQ_APP_ID, QQ_APP_KEY);
        qZoneSsoHandler.addToSocialSDK();
        mController.setShareContent(shareContent);

        CircleShareContent circleMedia = new CircleShareContent();
        circleMedia
                .setShareContent("来自BioInfo "+shareContent);
        circleMedia.setTitle("BioInfo 朋友圈");
        //circleMedia.setShareMedia(urlImage);

        // 设置QQ空间分享内容
        QZoneShareContent qzone = new QZoneShareContent();
            qzone.setShareContent("share test");
            qzone.setTargetUrl("http://www.umeng.com");
            qzone.setTitle("QZone title");
        //qzone.setShareMedia(urlImage);
        // qzone.setShareMedia(uMusic);
        //mController.setShareMedia(qzone);
    }

    private void addQQQZonePlatform() {
        String appId = QQ_APP_ID;
        String appKey = QQ_APP_KEY;
        // 添加QQ支持, 并且设置QQ分享内容的target url
        UMQQSsoHandler qqSsoHandler = new UMQQSsoHandler(mActivity,
                appId, appKey);
        qqSsoHandler.setTargetUrl("http://www.umeng.com/social");
        qqSsoHandler.addToSocialSDK();

        // 添加QZone平台
        QZoneSsoHandler qZoneSsoHandler = new QZoneSsoHandler(mActivity, appId, appKey);
        qZoneSsoHandler.addToSocialSDK();
    }

    /**
     * @功能描述 : 添加微信平台分享
     * @return
     */
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
}
