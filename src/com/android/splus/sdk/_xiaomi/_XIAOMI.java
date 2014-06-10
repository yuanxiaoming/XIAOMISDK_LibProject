package com.android.splus.sdk._xiaomi;

import com.android.splus.sdk.apiinterface.APIConstants;
import com.android.splus.sdk.apiinterface.DateUtil;
import com.android.splus.sdk.apiinterface.IPayManager;
import com.android.splus.sdk.apiinterface.InitBean;
import com.android.splus.sdk.apiinterface.InitBean.InitBeanSuccess;
import com.android.splus.sdk.apiinterface.InitCallBack;
import com.android.splus.sdk.apiinterface.LoginCallBack;
import com.android.splus.sdk.apiinterface.LoginParser;
import com.android.splus.sdk.apiinterface.LogoutCallBack;
import com.android.splus.sdk.apiinterface.MD5Util;
import com.android.splus.sdk.apiinterface.NetHttpUtil;
import com.android.splus.sdk.apiinterface.NetHttpUtil.DataCallback;
import com.android.splus.sdk.apiinterface.RechargeCallBack;
import com.android.splus.sdk.apiinterface.RequestModel;
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

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
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

    private int mUid;

    private String mPassport;

    private String mSessionid ;

    private int mMoney ;

    private String mPayway="_XIAOMI" ;

    private ProgressDialog mProgressDialog;

    private String mServerName;

    private Integer mRoleId;

    private String mRoleName;

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
            Message message=new Message();
            switch (code) {
                case MiErrorCode.MI_XIAOMI_GAMECENTER_SUCCESS: // 登陆成功
                    // 获取用户的登陆后的UID（即用户唯一标识）
                    String uid = String.valueOf(miaccountinfo.getUid());
                    // 获取用户的登陆的Session（请参考 2.1.5.3流程校验Session有效性）
                    String session = miaccountinfo.getSessionId();
                    // 获取用户的登陆的nikename
                    String nikename = miaccountinfo.getNikename();
                    // 请开发者完成将uid和session提交给开发者自己服务器进行session验证
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    Integer gameid = mInitBean.getGameid();
                    String partner = mInitBean.getPartner();
                    String referer = mInitBean.getReferer();
                    long unixTime = DateUtil.getUnixTime();
                    String deviceno=mInitBean.getDeviceNo();
                    String signStr =deviceno+gameid+partner+referer+unixTime+mInitBean.getAppKey();
                    String sign=MD5Util.getMd5toLowerCase(signStr);

                    params.put("deviceno", deviceno);
                    params.put("gameid", gameid);
                    params.put("partner",partner);
                    params.put("referer", referer);
                    params.put("time", unixTime);
                    params.put("sign", sign);
                    params.put("partner_sessionid", session);
                    params.put("partner_uid",  uid);
                    params.put("partner_token", "");
                    params.put("partner_nickname", nikename);
                    params.put("partner_username", "");
                    params.put("partner_appid", mAppId);
                    String hashMapTOgetParams = NetHttpUtil.hashMapTOgetParams(params, APIConstants.LOGIN_URL);
                    System.out.println(hashMapTOgetParams);

                    message.what=1;
                    message.obj=params;
                    loginHandler.sendMessage(message);
                    break;
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_LOGIN_FAIL:
                    // 登陆失败
                    message.what=2;
                    loginHandler.sendMessage(message);
                    break;
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_CANCEL:
                    // 取消登录
                    message.what=3;
                    loginHandler.sendMessage(message);
                    break;
                case MiErrorCode.MI_XIAOMI_GAMECENTER_ERROR_ACTION_EXECUTED:
                    // 登录操作正在进行中
                    break;
                default:
                    // 登录失败
                    mLoginCallBack.loginFaile("登录失败");
                    message.what=2;
                    loginHandler.sendMessage(message);
                    break;
            }

        }

    };
    private Handler loginHandler=new Handler(Looper.getMainLooper()){

        public void handleMessage(android.os.Message msg) {

            switch (msg.what) {
                case 1:
                    showProgressDialog(mActivity);
                    HashMap<String, Object>  params=(HashMap<String, Object>) msg.obj;
                    NetHttpUtil.getDataFromServerPOST(mActivity,new RequestModel(APIConstants.LOGIN_URL, params, new LoginParser()),mLoginDataCallBack);
                    break;
                case 2:
                    mLoginCallBack.loginFaile("登录失败");
                    break;
                case 3:
                    mLoginCallBack.backKey("登录取消");
                    break;
            }
        }
    };


    private DataCallback<JSONObject> mLoginDataCallBack = new DataCallback<JSONObject>() {

        @Override
        public void callbackSuccess(JSONObject paramObject) {
            closeProgressDialog();
            Log.d(TAG, "mLoginDataCallBack---------"+paramObject.toString());
            try {
                if (paramObject != null && paramObject.optInt("code") == 1) {
                    JSONObject data = paramObject.optJSONObject("data");
                    mUid = data.optInt("uid");
                    mPassport = data.optString("passport");
                    mSessionid = data.optString("sessionid");
                    mUserModel=new UserAccount() {

                        @Override
                        public Integer getUserUid() {
                            return mUid;

                        }

                        @Override
                        public String getUserName() {
                            return mPassport;

                        }

                        @Override
                        public String getSession() {
                            return mSessionid;

                        }
                    };
                    mLoginCallBack.loginSuccess(mUserModel);

                } else {
                    mLoginCallBack.loginFaile(paramObject.optString("msg"));
                }
            } catch (Exception e) {
                mLoginCallBack.loginFaile(e.getLocalizedMessage());
            }
        }

        @Override
        public void callbackError(String error) {
            closeProgressDialog();
            mLoginCallBack.loginFaile(error);
        }

    };

    @Override
    public void recharge(Activity activity, Integer serverId, String serverName, Integer roleId, String roleName, String outOrderid, String pext, RechargeCallBack rechargeCallBack) {
        rechargeByQuota(activity, serverId, serverName, roleId, roleName, outOrderid, pext, 0f, rechargeCallBack);
    }

    @Override
    public void rechargeByQuota(Activity activity, final Integer serverId, final String serverName, final Integer roleId, final String roleName, final String outOrderid, final String pext, Float money, RechargeCallBack rechargeCallBack) {
        this.mActivity = activity;
        this.mRechargeCallBack = rechargeCallBack;
        this.mMoney=money.intValue();
        this.mRoleId=roleId;
        this.mRoleName=roleName;
        this.mServerName=serverName;

        if (mMoney == 0) {
            final EditText editText = new EditText(activity);
            InputFilter[] filters = { new InputFilter.LengthFilter(6) };
            editText.setFilters( filters );
            editText.setInputType( InputType.TYPE_CLASS_NUMBER );
            new AlertDialog.Builder(activity)
            .setTitle("请输入金额")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setView(editText)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(TextUtils.isEmpty(editText.getText().toString())||editText.getText().toString().startsWith("0")){
                        Toast.makeText(mActivity, "请输入金额", Toast.LENGTH_SHORT).show();
                        return;
                    }else{
                        mMoney=Integer.parseInt(editText.getText().toString());
                        HashMap<String, Object> params = new HashMap<String, Object>();
                        Integer gameid = mInitBean.getGameid();
                        String partner = mInitBean.getPartner();
                        String referer = mInitBean.getReferer();
                        long unixTime = DateUtil.getUnixTime();
                        String deviceno=mInitBean.getDeviceNo();
                        String signStr =gameid+serverName+deviceno+referer+partner+mUid+mMoney+mPayway+unixTime+mInitBean.getAppKey();
                        String sign=MD5Util.getMd5toLowerCase(signStr);

                        params.put("deviceno", deviceno);
                        params.put("gameid", gameid);
                        params.put("partner",partner);
                        params.put("referer", referer);
                        params.put("time", unixTime);
                        params.put("sign", sign);
                        params.put("uid",mUid);
                        params.put("passport",mPassport);
                        params.put("serverId",serverId);
                        params.put("serverName",serverName);
                        params.put("roleId",roleId);
                        params.put("roleName",roleName);
                        params.put("money",mMoney);
                        params.put("pext",pext);
                        params.put("money",mMoney);
                        params.put("payway",mPayway);
                        params.put("outOrderid",outOrderid);
                        String hashMapTOgetParams = NetHttpUtil.hashMapTOgetParams(params, APIConstants.PAY_URL);
                        System.out.println(hashMapTOgetParams);
                        NetHttpUtil.getDataFromServerPOST(mActivity, new RequestModel(APIConstants.PAY_URL, params,new LoginParser()),mRechargeDataCallBack);
                    }

                }

            }).show();

        }else{
            HashMap<String, Object> params = new HashMap<String, Object>();
            Integer gameid = mInitBean.getGameid();
            String partner = mInitBean.getPartner();
            String referer = mInitBean.getReferer();
            long unixTime = DateUtil.getUnixTime();
            String deviceno=mInitBean.getDeviceNo();
            String signStr =gameid+serverName+deviceno+referer+partner+mUid+mMoney+mPayway+unixTime+mInitBean.getAppKey();
            String sign=MD5Util.getMd5toLowerCase(signStr);

            params.put("deviceno", deviceno);
            params.put("gameid", gameid);
            params.put("partner",partner);
            params.put("referer", referer);
            params.put("time", unixTime);
            params.put("sign", sign);
            params.put("uid",mUid);
            params.put("passport",mPassport);
            params.put("serverId",serverId);
            params.put("serverName",serverName);
            params.put("roleId",roleId);
            params.put("roleName",roleName);
            params.put("money",mMoney);
            params.put("pext",pext);
            params.put("money",mMoney);
            params.put("payway",mPayway);
            params.put("outOrderid",outOrderid);
            String hashMapTOgetParams = NetHttpUtil.hashMapTOgetParams(params, APIConstants.PAY_URL);
            System.out.println(hashMapTOgetParams);
            NetHttpUtil.getDataFromServerPOST(mActivity, new RequestModel(APIConstants.PAY_URL, params,new LoginParser()),mRechargeDataCallBack);
        }
    }

    private DataCallback<JSONObject> mRechargeDataCallBack = new DataCallback<JSONObject>() {

        @Override
        public void callbackSuccess(JSONObject paramObject) {
            Log.d(TAG, "mRechargeDataCallBack---------"+paramObject.toString());

            if (paramObject != null && (paramObject.optInt("code") == 1||paramObject.optInt("code") == 24)) {
                JSONObject data = paramObject.optJSONObject("data");
                String orderid=data.optString("orderid");
                MiBuyInfoOnline online = new MiBuyInfoOnline();
                online.setCpOrderId(UUID.randomUUID().toString());
                online.setCpUserInfo(orderid);
                online.setMiBi(mMoney);
                try {
                    Bundle mBundle = new Bundle();
                    mBundle.putString(GameInfoField.GAME_USER_BALANCE, "1000"); // 用户余额
                    mBundle.putString(GameInfoField.GAME_USER_GAMER_VIP, "vip0"); // vip等级
                    mBundle.putString(GameInfoField.GAME_USER_LV, "20"); // 角色等级
                    mBundle.putString(GameInfoField.GAME_USER_PARTY_NAME, "猎人"); // 工会，帮派
                    mBundle.putString(GameInfoField.GAME_USER_ROLE_NAME, mRoleName); // 角色名称
                    mBundle.putString(GameInfoField.GAME_USER_ROLEID, String.valueOf(mRoleId)); // 角色id
                    mBundle.putString(GameInfoField.GAME_USER_SERVER_NAME, mServerName); // 所在服务器
                    MiCommplatform.getInstance().miUniPayOnline(mActivity, online, mBundle, mOnPayProcessListener);
                } catch (Exception e) {
                    mRechargeCallBack.rechargeFaile(e.getLocalizedMessage());
                }

            }else {
                Log.d(TAG, paramObject.optString("msg"));
                mRechargeCallBack.rechargeFaile(paramObject.optString("msg"));
            }

        }

        @Override
        public void callbackError(String error) {
            Log.d(TAG, error);
            mRechargeCallBack.rechargeFaile(error);

        }

    };


    com.xiaomi.gamecenter.sdk.OnPayProcessListener mOnPayProcessListener = new com.xiaomi.gamecenter.sdk.OnPayProcessListener() {

        @Override
        public void finishPayProcess(int code) {
            switch (code) {
                case MiErrorCode.MI_XIAOMI_GAMECENTER_SUCCESS:
                    // 贩买成功
                    mRechargeCallBack.rechargeSuccess(mUserModel);
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
            // 不可用时候处理
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


    /**
     * @return void 返回类型
     * @Title: showProgressDialog(设置进度条)
     * @author xiaoming.yuan
     * @data 2013-7-12 下午10:09:36
     */
    protected void showProgressDialog(Activity activity) {
        if (! activity.isFinishing()) {
            try {
                this.mProgressDialog = new ProgressDialog(activity);// 实例化
                // 设置ProgressDialog 的进度条style
                this.mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置进度条风格，风格为圆形，旋转的
                this.mProgressDialog.setTitle("登陆");
                this.mProgressDialog.setMessage("加载中...");// 设置ProgressDialog 提示信息
                // 设置ProgressDialog 的进度条是否不明确
                this.mProgressDialog.setIndeterminate(false);
                // 设置ProgressDialog 的进度条是否不明确
                this.mProgressDialog.setCancelable(false);
                this.mProgressDialog.setCanceledOnTouchOutside(false);
                this.mProgressDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * @return void 返回类型
     * @Title: closeProgressDialog(关闭进度条)
     * @author xiaoming.yuan
     * @data 2013-7-12 下午10:09:30
     */
    protected void closeProgressDialog() {
        if (this.mProgressDialog != null && this.mProgressDialog.isShowing())
            this.mProgressDialog.dismiss();
    }
}
