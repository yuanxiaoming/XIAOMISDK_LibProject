package com.android.splus.sdk._xiaomi;

import com.android.splus.sdk.apiinterface.IPayManager;
import com.android.splus.sdk.apiinterface.InitBean;
import com.android.splus.sdk.apiinterface.InitBean.InitBeanSuccess;
import com.android.splus.sdk.apiinterface.InitCallBack;
import com.android.splus.sdk.apiinterface.LoginCallBack;
import com.android.splus.sdk.apiinterface.LogoutCallBack;
import com.android.splus.sdk.apiinterface.RechargeCallBack;
import com.android.splus.sdk.apiinterface.UserAccount;
import com.xiaomi.gamecenter.sdk.GameInfoField;
import com.xiaomi.gamecenter.sdk.MiCommplatform;
import com.xiaomi.gamecenter.sdk.MiErrorCode;
import com.xiaomi.gamecenter.sdk.MiSdkAction;
import com.xiaomi.gamecenter.sdk.entry.MiAccountInfo;
import com.xiaomi.gamecenter.sdk.entry.MiAppInfo;
import com.xiaomi.gamecenter.sdk.entry.MiBuyInfoOnline;
import com.xiaomi.gamecenter.sdk.entry.MiGameType;
import com.xiaomi.gamecenter.sdk.entry.PayMode;
import com.xiaomi.gamecenter.sdk.entry.ScreenOrientation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Properties;
import java.util.UUID;

public class _XIAOMI implements IPayManager {

    private static final String TAG = "_XIAOMI";

    private static _XIAOMI _mXIAOMI;

    // 平台参数
    private Properties mProperties;

    private String mAppId;

    private String mAppkey;

    private InitBean mInitBean;

    private InitCallBack mInitCallBack;

    private Activity mActivity;

    private LoginCallBack mLoginCallBack;

    private RechargeCallBack mRechargeCallBack;

    private LogoutCallBack mLogoutCallBack;

    // 下面参数仅在测试时用
    private UserAccount mUserModel;

    private int mUid = 0;

    private String mPassport = "";

    private String mSessionid = "";

    private Integer  mibi=0 ;

    /**
     * @Title: _XIAOMI
     * @Description:( 将构造函数私有化)
     */
    private _XIAOMI() {

    }

    /**
     * @Title: getInstance(获取实例)
     * @author xiaoming.yuan
     * @data 2014-2-26 下午2:30:02
     * @return _XIAOMI 返回类型
     */
    public static _XIAOMI getInstance() {

        if (_mXIAOMI == null) {
            synchronized (_XIAOMI.class) {
                if (_mXIAOMI == null) {
                    _mXIAOMI = new _XIAOMI();
                }
            }
        }
        return _mXIAOMI;
    }

    @Override
    public void setInitBean(InitBean bean) {
        this.mInitBean = bean;
        this.mProperties = mInitBean.getProperties();
        if (mProperties != null) {
            mAppId = mProperties.getProperty("xiaomi_appid") == null ? "0" : mProperties.getProperty("xiaomi_appid");
            mAppkey = mProperties.getProperty("xiaomi_appkey") == null ? "0" : mProperties.getProperty("xiaomi_appkey");
        }
    }

    @Override
    public void init(Activity activity, Integer gameid, String appkey, InitCallBack initCallBack, boolean useUpdate, Integer orientation) {

        this.mInitCallBack = initCallBack;
        this.mActivity = activity;
        mInitBean.initSplus(activity, initCallBack, new InitBeanSuccess() {
            @Override
            public void initBeaned(boolean initBeanSuccess) {
                MiAppInfo appInfo = new MiAppInfo();
                appInfo.setAppId(Integer.parseInt(mAppId));
                appInfo.setAppKey(mAppkey);
                appInfo.setAppType(MiGameType.online); // 网游
                appInfo.setPayMode(PayMode.custom);
                if (mInitBean.getOrientation() == Configuration.ORIENTATION_PORTRAIT) {
                    appInfo.setOrientation(ScreenOrientation.vertical);
                } else {
                    appInfo.setOrientation(ScreenOrientation.horizontal);
                }
                MiCommplatform.Init(mActivity, appInfo);
                mInitCallBack.initSuccess("初始化完成", null);

            }
        });
    }

    @Override
    public void login(Activity activity, LoginCallBack loginCallBack) {
        this.mActivity = activity;
        this.mLoginCallBack = loginCallBack;

        MiCommplatform.getInstance().miLogin(activity, mLoginProcessListener);

    }

    com.xiaomi.gamecenter.sdk.OnLoginProcessListener mLoginProcessListener = new com.xiaomi.gamecenter.sdk.OnLoginProcessListener() {

        @Override
        public void finishLoginProcess(int code, MiAccountInfo miaccountinfo) {

            switch (code) {
                case MiErrorCode.MI_XIAOMI_GAMECENTER_SUCCESS: // 登陆成功
                    // 获取用户的登陆后的UID（即用户唯一标识）
                    String uid = String.valueOf(miaccountinfo.getUid());
                    // 获取用户的登陆的Session（请参考 2.1.5.3流程校验Session有效性）
                    String session = miaccountinfo.getSessionId();
                    // 获取用户的登陆的nikename
                    String nikename = miaccountinfo.getNikename();
                    // 请开发者完成将uid和session提交给开发者自己服务器进行session验证
                    mLoginCallBack.loginSuccess(null);
                    break;
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_LOGIN_FAIL:
                    // 登陆失败
                    mLoginCallBack.loginFaile("登录失败");
                    break;
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_CANCEL:
                    // 取消登录
                    mLoginCallBack.backKey("取消登录");
                    break;
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_ACTION_EXECUTED:
                    // 登录操作正在进行中
                    break;
                default:
                    // 登录失败
                    mLoginCallBack.loginFaile("登录失败");
                    break;
            }

        }

    };

    @Override
    public void recharge(Activity activity, Integer serverId, String serverName, Integer roleId, String roleName, String outOrderid, String pext, RechargeCallBack rechargeCallBack) {
        rechargeByQuota(activity, serverId, serverName, roleId, roleName, outOrderid, pext, 0f, rechargeCallBack);
    }

    @Override
    public void rechargeByQuota(Activity activity, Integer serverId, final String serverName, final Integer roleId, final String roleName, final String outOrderid, String pext, Float money, RechargeCallBack rechargeCallBack) {
        this.mActivity = activity;
        this.mRechargeCallBack = rechargeCallBack;
        this.mibi=0;
        if (money == 0) {
            final EditText editText = new EditText(activity);
            InputFilter[] filters = { new InputFilter.LengthFilter(6) };
            editText.setFilters( filters );
            editText.setInputType( InputType.TYPE_CLASS_NUMBER );
            new AlertDialog.Builder(activity).setTitle("请输入金额").setIcon(android.R.drawable.ic_dialog_info).setView(editText).setNegativeButton("取消", null)
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(TextUtils.isEmpty(editText.getText().toString())||editText.getText().toString().startsWith("0")){
                        Toast.makeText(mActivity, "请输入金额", Toast.LENGTH_SHORT).show();
                        return;
                    }else{
                        mibi=Integer.parseInt(editText.getText().toString());
                        MiBuyInfoOnline online = new MiBuyInfoOnline();
                        online.setCpOrderId(UUID.randomUUID().toString());
                        online.setCpUserInfo(outOrderid);
                        online.setMiBi(mibi);
                        try {
                            Bundle mBundle = new Bundle();
                            mBundle.putString(GameInfoField.GAME_USER_BALANCE, "1000"); // 用户余额
                            mBundle.putString(GameInfoField.GAME_USER_GAMER_VIP, "vip0"); // vip等级
                            mBundle.putString(GameInfoField.GAME_USER_LV, "20"); // 角色等级
                            mBundle.putString(GameInfoField.GAME_USER_PARTY_NAME, "猎人"); // 工会，帮派
                            mBundle.putString(GameInfoField.GAME_USER_ROLE_NAME, roleName); // 角色名称
                            mBundle.putString(GameInfoField.GAME_USER_ROLEID, String.valueOf(roleId)); // 角色id
                            mBundle.putString(GameInfoField.GAME_USER_SERVER_NAME, serverName); // 所在服务器
                            MiCommplatform.getInstance().miUniPayOnline(mActivity, online, mBundle, mOnPayProcessListener);
                        } catch (Exception e) {
                            mRechargeCallBack.rechargeFaile(e.getLocalizedMessage());
                        }

                    }

                }

            }).show();

        }else{
            mibi=money.intValue();
            MiBuyInfoOnline online = new MiBuyInfoOnline();
            online.setCpOrderId(UUID.randomUUID().toString());
            online.setCpUserInfo(outOrderid);
            online.setMiBi(mibi);
            try {
                Bundle mBundle = new Bundle();
                mBundle.putString(GameInfoField.GAME_USER_BALANCE, "1000"); // 用户余额
                mBundle.putString(GameInfoField.GAME_USER_GAMER_VIP, "vip0"); // vip等级
                mBundle.putString(GameInfoField.GAME_USER_LV, "20"); // 角色等级
                mBundle.putString(GameInfoField.GAME_USER_PARTY_NAME, "猎人"); // 工会，帮派
                mBundle.putString(GameInfoField.GAME_USER_ROLE_NAME, roleName); // 角色名称
                mBundle.putString(GameInfoField.GAME_USER_ROLEID, String.valueOf(roleId)); // 角色id
                mBundle.putString(GameInfoField.GAME_USER_SERVER_NAME, serverName); // 所在服务器
                MiCommplatform.getInstance().miUniPayOnline(activity, online, mBundle, mOnPayProcessListener);

            } catch (Exception e) {
                rechargeCallBack.rechargeFaile(e.getLocalizedMessage());
            }

        }

    }

    com.xiaomi.gamecenter.sdk.OnPayProcessListener mOnPayProcessListener = new com.xiaomi.gamecenter.sdk.OnPayProcessListener() {

        @Override
        public void finishPayProcess(int code) {
            switch (code) {
                case MiErrorCode.MI_XIAOMI_GAMECENTER_SUCCESS:
                    // 贩买成功

                    mRechargeCallBack.rechargeSuccess(null);
                    break;
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_PAY_CANCEL:
                    // 取消贩买
                    mRechargeCallBack.backKey("取消贩买");
                    break;
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_PAY_FAILURE:
                    // 贩买失败
                    mRechargeCallBack.rechargeFaile("贩买失败");
                    break;
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_ACTION_EXECUTED:
                    // 操作正在进行中
                    break;
                default:
                    // 贩买失败
                    mRechargeCallBack.rechargeFaile("贩买失败");
                    break;

            }

        }
    };

    @Override
    public void exitSDK() {

    }

    @Override
    public void logout(Activity activity, LogoutCallBack logoutCallBack) {
        this.mLogoutCallBack = logoutCallBack;
        this.mActivity = activity;
        MiCommplatform.getInstance().miLogout(mLogoutProcessListener);

    }

    com.xiaomi.gamecenter.sdk.OnLoginProcessListener mLogoutProcessListener = new com.xiaomi.gamecenter.sdk.OnLoginProcessListener() {

        @Override
        public void finishLoginProcess(int code, MiAccountInfo miaccountinfo) {
            switch (code) {
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_LOGINOUT_SUCCESS:
                    mLogoutCallBack.logoutCallBack();
                    break;
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_LOGINOUT_FAIL:
                    mLogoutCallBack.logoutCallBack();
                    break;
                default:
                    mLogoutCallBack.logoutCallBack();
                    break;
            }

        }
    };

    @Override
    public void setDBUG(boolean logDbug) {
    }

    @Override
    public void enterUserCenter(Activity activity, LogoutCallBack logoutCallBack) {
        this.mActivity = activity;
        this.mLogoutCallBack = logoutCallBack;
        // 需判断入口是否可用
        final boolean canOpen = MiCommplatform.getInstance().canOpenEntrancePage();
        if (canOpen) {
            // 入口可用，打开界面
            Intent intent = new Intent();
            intent.setAction(MiSdkAction.SDK_ACTION_ENTRANCE);
            activity.startActivity(intent);
        } else {
            // TODO 不可用时候处理
            Toast.makeText(activity, "入口不可用,请升级小米游戏安全插件到最新版本", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void sendGameStatics(Activity activity, Integer serverId, String serverName, Integer roleId, String roleName, String level) {
    }

    @Override
    public void enterBBS(Activity activity) {
    }

    @Override
    public void creatFloatButton(Activity activity, boolean showlasttime, int align, float position) {

    }

    @Override
    public void onResume(Activity activity) {

    }

    @Override
    public void onPause(Activity activity) {

    }

    @Override
    public void onStop(Activity activity) {
    }

    @Override
    public void onDestroy(Activity activity) {

    }
}
