package tw.idv.palatis.xscreenshotfix;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import androidx.annotation.Keep;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

@Keep
public class Hook implements IXposedHookLoadPackage {
    private static final String LOG_TAG = "XScreenshotFix";

    private static final String VICTIM_APP = "com.android.systemui";
    private static final String VICTIM_PROCESS = VICTIM_APP + ":screenshot";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!VICTIM_APP.equals(lpparam.packageName))
            return;
        if (!VICTIM_PROCESS.equals(lpparam.processName))
            return;

        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.O:
            case Build.VERSION_CODES.O_MR1:
            case Build.VERSION_CODES.P:
                handleLoadPackage_Oreo(lpparam);
                break;
        }
    }

    private void handleLoadPackage_Oreo(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        final Class<?> c_SaveImageInBackgroundTask = findClass("com.android.systemui.screenshot.SaveImageInBackgroundTask", lpparam.classLoader);
        final Method m_doInBackground = c_SaveImageInBackgroundTask.getDeclaredMethod("doInBackground", Void[].class);
        final Field f_mScreenshotDir = c_SaveImageInBackgroundTask.getDeclaredField("mScreenshotDir");
        final Field f_mImageFileName = c_SaveImageInBackgroundTask.getDeclaredField("mImageFileName");
        final Field f_mImageFilePath = c_SaveImageInBackgroundTask.getDeclaredField("mImageFilePath");

        m_doInBackground.setAccessible(true);
        f_mScreenshotDir.setAccessible(true);
        f_mImageFileName.setAccessible(true);
        f_mImageFilePath.setAccessible(true);

        hookMethod(
                m_doInBackground,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        final String mImageFileName = (String) f_mImageFileName.get(param.thisObject);
                        final String validImageFileName = URLEncoder.encode(mImageFileName, StandardCharsets.UTF_8.name());
                        if (!TextUtils.equals(mImageFileName, validImageFileName)) {
                            Log.d(LOG_TAG, "Rewrite screenshot filename: " + mImageFileName + " => " + validImageFileName);
                            f_mImageFileName.set(param.thisObject, validImageFileName);
                            f_mImageFilePath.set(param.thisObject, new File((File) f_mScreenshotDir.get(param.thisObject), validImageFileName).getAbsolutePath());
                        }
                    }
                }
        );
    }
}
