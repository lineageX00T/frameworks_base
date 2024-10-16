package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.settingslib.net.DataUsageController;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;

import java.util.List;

public class DataUsageView extends TextView {

    private static boolean shouldUpdateData;
    private static boolean shouldUpdateDataTextView;
    private ConnectivityManager mConnectivityManager;
    private NetworkController mNetworkController;
    private Context mContext;
    private String formattedInfo;

    public DataUsageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mNetworkController = Dependency.get(NetworkController.class);
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static void updateUsage() {
        shouldUpdateData = true;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (shouldUpdateData) {
            shouldUpdateData = false;
            AsyncTask.execute(this::updateUsageData);
        }
        if (shouldUpdateDataTextView) {
            shouldUpdateDataTextView = false;
            setText(formattedInfo);
        }
    }

    private void updateUsageData() {
        boolean showDailyDataUsage = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.DATA_USAGE_PERIOD, 1) == 0;
        DataUsageController dataController = new DataUsageController(mContext);
        DataUsageController.DataUsageInfo info;
        String prefix;
        String suffix;
        if (isWifiConnected()) {
            info = showDailyDataUsage ? dataController.getDailyDataUsageInfo()
                    : dataController.getWifiDataUsageInfo();
            prefix = mContext.getResources().getString(R.string.usage_wifi_prefix);
        } else {
            dataController.setSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId());
            info = showDailyDataUsage ? dataController.getDailyDataUsageInfo()
                    : dataController.getDataUsageInfo();
            prefix = getSlotCarrierName();
        }
        suffix = showDailyDataUsage ? mContext.getResources().getString(R.string.usage_data_today) 
                  : mContext.getResources().getString(R.string.usage_data);
        formattedInfo = prefix + ": " + formatDataUsage(info.usageLevel) + " " + suffix;
        shouldUpdateDataTextView = true;
    }

    private CharSequence formatDataUsage(long byteValue) {
        final BytesResult res = Formatter.formatBytes(mContext.getResources(), byteValue,
                Formatter.FLAG_IEC_UNITS);
        return BidiFormatter.getInstance().unicodeWrap(mContext.getString(
                com.android.internal.R.string.fileSizeSuffix, res.value, res.units));
    }

    private String getSlotCarrierName() {
        CharSequence result = mContext.getResources().getString(R.string.usage_data_prefix);
        SubscriptionManager subManager = mContext.getSystemService(SubscriptionManager.class);
        int subId = subManager.getDefaultDataSubscriptionId();
        List<SubscriptionInfo> subInfoList =
                subManager.getActiveSubscriptionInfoList(true);
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (subId == subInfo.getSubscriptionId()) {
                    result = subInfo.getDisplayName();
                    break;
                }
            }
        }
        return result.toString();
    }

    private boolean isWifiConnected() {
        Network network = mConnectivityManager.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            return false;
        }
    }

}
