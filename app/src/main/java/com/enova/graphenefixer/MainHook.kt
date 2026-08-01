package com.enova.graphenefixer

import android.content.pm.ApplicationInfo
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "android" && lpparam.packageName != "com.android.settings") return

        val DCL_MEM = "android.ext.settings.app.AswRestrictMemoryDynCodeLoading"
        val DCL_STORAGE = "android.ext.settings.app.AswRestrictStorageDynCodeLoading"
        val DCL_CLASSES = arrayOf(
            DCL_MEM, DCL_STORAGE
        )

        val replacement = object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam?): Any? {
                val appInfo = param?.args?.getOrNull(2) as? ApplicationInfo

                // Settings has to always be allowed DCL via memory so that it can be patched as well
                if (appInfo != null && appInfo.packageName == "com.android.settings" && param.method?.declaringClass?.name == DCL_MEM) {
                    return false
                }

                // All system apps are configurable (null)
                if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                    return null
                }

                // Defer to original implementation for other apps
                return XposedBridge.invokeOriginalMethod(
                    param?.method, param?.thisObject, param?.args
                )
            }
        }

        for (className in DCL_CLASSES) {
            val dclClass =
                try {
                    XposedHelpers.findClass(className, lpparam.classLoader)
                } catch (_: XposedHelpers.ClassNotFoundError) {
                    // Class not found, we are not on GrapheneOS
                    return
                }
            XposedBridge.hookAllMethods(dclClass, "getImmutableValue", replacement)
        }
    }
}
