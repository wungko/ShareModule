package com.share.module;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzoneShare;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;


/**
 * Created by wangke on 2017/2/26.
 * 分享弹窗
 */

public class ShareDialog {

    public static ShareDialog newInstance(Activity activity, String action, ShareEntity shareEntity, QQShareListener qqShareListener) {
        return new ShareDialog(activity, action, shareEntity, qqShareListener);
    }

    /**
     * 分享action  用于回调处理业务逻辑
     **/
    private String mAction;
    private Dialog mDialog;
    private Activity mActivity;
    private boolean isTop = false;
    ShareEntity mShareEntity;
    private IUiListener mIUiListener;

    private ShareDialog(Activity activity, String action, final ShareEntity shareEntity, QQShareListener qqShareListener) {
        this.mAction = action;
        this.mActivity = activity;
        this.mShareEntity = shareEntity;
        this.mIUiListener = qqShareListener;
        mDialog = new Dialog(mActivity, R.style.style_dialog_share);
        mDialog.setCanceledOnTouchOutside(true);
        mDialog.setContentView(R.layout.view_share);
        mDialog.findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel();
            }
        });

        mDialog.findViewById(R.id.tv_share_qq).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Tencent tencent = getTencent();
                final Bundle params = new Bundle();
                params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
                params.putString(QQShare.SHARE_TO_QQ_TITLE, mShareEntity.shareTitle);
                params.putString(QQShare.SHARE_TO_QQ_SUMMARY, mShareEntity.summary);
                params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, mShareEntity.targetUrl);
                params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, mShareEntity.imageUrl);
                tencent.shareToQQ(mActivity, params, mIUiListener);
                cancel();
            }
        });
        mDialog.findViewById(R.id.tv_share_qqzone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //分享类型
                Tencent tencent = getTencent();
                final Bundle params = new Bundle();
                params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
                params.putString(QzoneShare.SHARE_TO_QQ_TITLE, mShareEntity.shareTitle);//必填
                params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, mShareEntity.summary);//选填
                params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, mShareEntity.targetUrl);//必填
                params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, mShareEntity.imageUrls);
                tencent.shareToQzone(mActivity, params, mIUiListener);
                cancel();
            }
        });
        mDialog.findViewById(R.id.tv_share_wechat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendWx(shareEntity, SendMessageToWX.Req.WXSceneSession);
                cancel();

            }
        });
        mDialog.findViewById(R.id.tv_share_moment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendWx(shareEntity, SendMessageToWX.Req.WXSceneTimeline);
                cancel();
            }
        });
    }


    private void sendWx(final ShareEntity shareEntity, final int type) {
        final IWXAPI api = WXAPIFactory.createWXAPI(mActivity, "wxf45267a8556aca57", true);
        api.registerApp("wxf45267a8556aca57");

        WXWebpageObject object = new WXWebpageObject();
        object.webpageUrl = shareEntity.targetUrl;
        final WXMediaMessage msg = new WXMediaMessage(object);
        msg.title = shareEntity.shareTitle;
        msg.description = shareEntity.summary;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bmp = BitmapFactory.decodeStream(new URL(shareEntity.imageUrl).openStream());
                    if (bmp == null) {
                        return;
                    }

                    Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true);
                    bmp.recycle();
                    msg.thumbData = bmpToByteArray(thumbBmp,true);  //设置缩略图


                    final SendMessageToWX.Req req = new SendMessageToWX.Req();
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            switch (type) {
                                case SendMessageToWX.Req.WXSceneSession:
                                    req.transaction = "friend";
                                    req.scene = SendMessageToWX.Req.WXSceneSession;
                                    break;
                                case SendMessageToWX.Req.WXSceneTimeline:
                                    req.transaction = "moment";
                                    req.scene = SendMessageToWX.Req.WXSceneTimeline;
                                    break;
                            }

                            req.message = msg;
                            api.sendReq(req);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }).start();



    }

    public void show() {
        if (mDialog != null) {
            Window wm = this.mDialog.getWindow();
            WindowManager m = wm.getWindowManager();
            Display d = m.getDefaultDisplay();
            WindowManager.LayoutParams p = wm.getAttributes();
            p.width = d.getWidth();
            if (this.isTop) {
                p.gravity = Gravity.TOP;
            } else {
                p.gravity = Gravity.BOTTOM;
            }
            wm.setWindowAnimations(R.style.style_bottom_top);
            wm.setAttributes(p);
            mDialog.show();
        }
    }

    private Tencent getTencent() {
        return Tencent.createInstance("1105731731", mActivity.getApplicationContext());
    }

    public void cancel() {
        if (mDialog != null) {
            mDialog.cancel();
        }
    }

    public void setTopIfNecessary() {
        this.isTop = true;
    }

    public interface QQShareListener extends IUiListener {
    }

    public static class ShareEntity {
        public String shareTitle;
        public String summary;
        public String targetUrl;
        public String imageUrl;
        public ArrayList<String> imageUrls;

    }

    private byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


}
