package moe.ono.hooks.ui;

import static moe.ono.config.CacheConfig.isSetEntry;
import static moe.ono.config.CacheConfig.setIsSetEntry;
import static moe.ono.util.Initiator.loadClass;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function0;
import moe.ono.R;
import moe.ono.activity.OUOSettingActivity;
import moe.ono.config.CacheConfig;
import moe.ono.hooks.XHook;
import moe.ono.hooks._base.BaseSwitchFunctionHookItem;
import moe.ono.hooks._core.annotation.HookItem;
import moe.ono.lifecycle.Parasitics;
import moe.ono.reflex.Reflex;
import moe.ono.startup.HookBase;
import moe.ono.util.Initiator;
import moe.ono.util.Logger;

@HookItem(path = "设置模块入口", description = "")
public class MainSettingEntranceInjector extends BaseSwitchFunctionHookItem {
    private void injectSettingEntryForMainSettingConfigProvider() throws ReflectiveOperationException {
        // 8.9.70+
        Class<?> kMainSettingFragment = Initiator.load("com.tencent.mobileqq.setting.main.MainSettingFragment");
        if (kMainSettingFragment != null) {
            Class<?> kMainSettingConfigProvider = Initiator.loadClass("com.tencent.mobileqq.setting.main.MainSettingConfigProvider");
            // 9.1.20+, NewSettingConfigProvider, A/B test on 9.1.20
            Class<?> kNewSettingConfigProvider = Initiator.load("com.tencent.mobileqq.setting.main.NewSettingConfigProvider");
            Method getItemProcessListOld = Reflex.findSingleMethod(kMainSettingConfigProvider, List.class, false, Context.class);
            Method getItemProcessListNew = null;
            if (kNewSettingConfigProvider != null) {
                getItemProcessListNew = Reflex.findSingleMethod(kNewSettingConfigProvider, List.class, false, Context.class);
            }
            Class<?> kAbstractItemProcessor = Initiator.loadClass("com.tencent.mobileqq.setting.main.processor.AccountSecurityItemProcessor").getSuperclass();
            // SimpleItemProcessor has too few xrefs. I have no idea how to find it without a list of candidates.
            final String[] possibleSimpleItemProcessorNames = new String[]{
                    // 8.9.70 ~ 9.0.0
                    "com.tencent.mobileqq.setting.processor.g",
                    // 9.0.8+
                    "com.tencent.mobileqq.setting.processor.h",
                    // QQ 9.1.28.21880 (8398) gray
                    "as3.i",
            };
            List<Class<?>> possibleSimpleItemProcessorCandidates = new ArrayList<>(4);
            for (String name : possibleSimpleItemProcessorNames) {
                Class<?> klass = Initiator.load(name);
                if (klass != null && klass.getSuperclass() == kAbstractItemProcessor) {
                    possibleSimpleItemProcessorCandidates.add(klass);
                }
            }
            // assert possibleSimpleItemProcessorCandidates.size() == 1;
            if (possibleSimpleItemProcessorCandidates.size() != 1) {
                throw new IllegalStateException("possibleSimpleItemProcessorCandidates.size() != 1, got " + possibleSimpleItemProcessorCandidates);
            }
            Class<?> kSimpleItemProcessor = possibleSimpleItemProcessorCandidates.get(0);
            Method setOnClickListener;
            {
                List<Method> candidates = ArraysKt.filter(kSimpleItemProcessor.getDeclaredMethods(), m -> {
                    Class<?>[] argt = m.getParameterTypes();
                    // NOSONAR java:S1872 not same class
                    return m.getReturnType() == void.class && argt.length == 1 && Function0.class.getName().equals(argt[0].getName());
                });
                candidates.sort(Comparator.comparing(Method::getName));
                // TIM 4.0.95.4001 only have one method, that is the one we need (onClick() lambda)
                if (candidates.size() != 2 && candidates.size() != 1) {
                    throw new IllegalStateException("com.tencent.mobileqq.setting.processor.g.?(Function0)V candidates.size() != 1|2");
                }
                // take the smaller one
                setOnClickListener = candidates.get(0);
            }
            XC_MethodHook callback = getXcMethodHook(kSimpleItemProcessor, setOnClickListener);
            XposedBridge.hookMethod(getItemProcessListOld, callback);
            if (getItemProcessListNew != null) {
                XposedBridge.hookMethod(getItemProcessListNew, callback);
            }
        }
    }

    @NonNull
    private XC_MethodHook getXcMethodHook(Class<?> kSimpleItemProcessor, Method setOnClickListener) throws NoSuchMethodException {
        Constructor<?> ctorSimpleItemProcessor = kSimpleItemProcessor.getDeclaredConstructor(Context.class, int.class, CharSequence.class, int.class);
        XC_MethodHook callback = XHook.afterAlways(50, param -> {
            List<Object> result = (List<Object>) param.getResult();
            Context ctx = (Context) param.args[0];
            Class<?> kItemProcessorGroup = result.get(0).getClass();
            Constructor<?> ctor = kItemProcessorGroup.getDeclaredConstructor(List.class, CharSequence.class, CharSequence.class);
            Parasitics.injectModuleResources(ctx.getResources());
            @SuppressLint("DiscouragedApi")
            int resId = ctx.getResources().getIdentifier("qui_tuning", "drawable", ctx.getPackageName());
            Object entryItem = ctorSimpleItemProcessor.newInstance(ctx, R.id.OnO_settingEntryItem, "ONO", resId);
            Class<?> thatFunction0 = setOnClickListener.getParameterTypes()[0];
            Object theUnit = thatFunction0.getClassLoader().loadClass("kotlin.Unit").getField("INSTANCE").get(null);
            ClassLoader hostClassLoader = Initiator.getHostClassLoader();
            Object func0 = Proxy.newProxyInstance(hostClassLoader, new Class<?>[]{thatFunction0}, (proxy, method, args) -> {
                if (method.getName().equals("invoke")) {
                    onSettingEntryClick(ctx);
                    return theUnit;
                }
                // must be sth from Object
                return method.invoke(this, args);
            });
            setOnClickListener.invoke(entryItem, func0);
            ArrayList<Object> list = new ArrayList<>(1);
            list.add(entryItem);
            Object group = ctor.newInstance(list, "", "");
            boolean isNew = param.thisObject.getClass().getName().contains("NewSettingConfigProvider");
            int indexToInsert = isNew ? 2 : 1;
            result.add(indexToInsert, group);
        });
        return callback;
    }


    private void onSettingEntryClick(@NonNull Context context) {
        Intent intent = new Intent(context, OUOSettingActivity.class);
        context.startActivity(intent);
    }


    @Override
    public void load(@NonNull ClassLoader classLoader) throws Throwable {
        if (CacheConfig.isSetEntry()){
            return;
        }
        try {
            injectSettingEntryForMainSettingConfigProvider();
            CacheConfig.setIsSetEntry(true);
        } catch (ReflectiveOperationException e) {
            Logger.e("MainSettingEntranceInjector", e);
        }
    }
}