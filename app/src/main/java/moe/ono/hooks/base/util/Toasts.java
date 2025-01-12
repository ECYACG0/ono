package moe.ono.hooks.base.util;

import static moe.ono.util.Initiator.loadClass;
import static moe.ono.util.SyncUtils.runOnUiThread;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

import moe.ono.hooks._base.BaseFunctionHookItem;
import moe.ono.hooks._base.BaseHookItem;
import moe.ono.hooks._base.BaseSwitchFunctionHookItem;
import moe.ono.hooks._core.annotation.HookItem;
import moe.ono.startup.HookBase;
import moe.ono.util.Logger;

@HookItem(path = "Toasts", description = "")
public class Toasts extends BaseFunctionHookItem {
    public static String method_name = "Toasts";

    private static Method showQQToastInUiThreadMethod;

    public static final int TYPE_INFO = 0;
    public static final int TYPE_ERROR = 1;
    public static final int TYPE_SUCCESS = 2;

    public static void show(Context ctx, int type, @NonNull CharSequence text) {
        runOnUiThread(() -> {
            try {
                showQQToastInUiThreadMethod.invoke(null, type, text);
            } catch (Throwable e) {
                Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
            }
        });

    }

    public static void info(Context ctx, @NonNull CharSequence text) {
        show(ctx, TYPE_INFO, text);
    }

    public static void success(Context ctx, @NonNull CharSequence text) {
        show(ctx, TYPE_SUCCESS, text);
    }

    public static void error(Context ctx, @NonNull CharSequence text) {
        show(ctx, TYPE_ERROR, text);
    }

    @Override
    public void load(@NonNull ClassLoader classLoader) throws Throwable {
        try {
            Class<?> QQToastUtilClass = loadClass("com.tencent.util.QQToastUtil");
            showQQToastInUiThreadMethod = QQToastUtilClass.getDeclaredMethod("showQQToastInUiThread", int.class, String.class);
        } catch (Exception e) {
            Logger.e("QQToastUtil Not Found : ", e);
        }
    }
}