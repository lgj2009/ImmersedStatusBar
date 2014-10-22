package com.kqstone.immersedstatusbar;

import com.kqstone.immersedstatusbar.BitMapColor.Type;
import com.kqstone.immersedstatusbar.Utils.WindowType;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.provider.Settings;
import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class OnWindowFocusedHook extends XC_MethodHook {

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		Utils.log("windows focus changed to " + param.args[0]);
		if(!(Boolean)param.args[0])
			return;	
		
		final Activity activity = (Activity) param.thisObject;
		sendChangeStatusBarIntent(activity);
	}
	
	public static void sendChangeStatusBarIntent(final Activity activity) {
		boolean isSysApp = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mIsSystemApp");
		if (isSysApp) {
			Utils.log("System app, change color to transparent");
			return;
		} 
		
		WindowType type = Utils.getWindowType(activity);
		switch (type) {
		case Normal:
			boolean needGetColorFromBackground = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mNeedGetColorFromBackground");
			if (!needGetColorFromBackground)
				return;
			int delay = Constant.DELAY_GET_CACHEDRAWABLE;
			String activityName = activity.getLocalClassName();
			if (activityName.equals("com.uc.browser.InnerUCMobile")) {
				delay = 800;
			} else if (activityName.equals("activity.SplashActivity") && activity.getPackageName().equals("com.tencent.mobileqq")) {
				delay = 300;
			}
			
			Handler handler = new Handler();
			
			
			handler.postDelayed(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					Bitmap bitmap = Utils.getBitMapFromActivityBackground(activity);
					int color = Color.BLACK;
					boolean isdark = false;
					boolean darkHandled = false;
					
					if (bitmap != null) {
						BitMapColor bitmapColor = Utils.getBitmapColor(bitmap);
						if (bitmapColor.mType == Type.FLAT) {
							Utils.log("Flat BitMap found...");
							color = bitmapColor.Color;
							XposedHelpers.setAdditionalInstanceField(activity, "mStatusBarBackground", color);
							isdark = Utils.getDarkMode(color);
							XposedHelpers.setAdditionalInstanceField(activity, "mDarkMode", isdark);
							darkHandled = true;
						} else if (bitmapColor.mType == Type.GRADUAL) {
							Utils.log("GRADUAL BitMap found, rePadding viewgroup...");
							color = bitmapColor.Color;
							XposedHelpers.setAdditionalInstanceField(activity, "mStatusBarBackground", color);
							isdark = Utils.getDarkMode(color);
							XposedHelpers.setAdditionalInstanceField(activity, "mDarkMode", isdark);
							darkHandled = true;
							if (!(Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mRepaddingHandled")) {
								Utils.resetPadding(activity, Constant.OFFEST_FOR_GRADUAL_ACTIVITY);
								XposedHelpers.setAdditionalInstanceField(activity, "mRepaddingHandled", true);
							}
							
						} else if (bitmapColor.mType == Type.PICTURE) {
							Utils.log("Flat BitMap found...");
							if (Settings.System.getInt(
									activity.getContentResolver(),
									SettingsActivity.KEY_PREF_FORCE_TINT, 0) == 1) {
								color = bitmapColor.Color;
								XposedHelpers.setAdditionalInstanceField(activity,
										"mStatusBarBackground", color);
								isdark = Utils.getDarkMode(color);
								XposedHelpers.setAdditionalInstanceField(activity,
										"mDarkMode", isdark);
								darkHandled = true;
							}
						}
					}
					
					Intent intent = new Intent(Constant.INTENT_CHANGE_STATUSBAR_COLOR);
					intent.putExtra(Constant.STATUSBAR_BACKGROUND_COLOR, color);
					intent.putExtra(Constant.IS_DARKMODE, isdark);
					intent.putExtra(Constant.DARKMODE_HANDLE, darkHandled);

					activity.sendBroadcast(intent);
					XposedHelpers.setAdditionalInstanceField(activity, "mNeedGetColorFromBackground", false);
				}}, delay);
			break;
		case Translucent:
			boolean needGetDarkModeFromBackground = (Boolean) XposedHelpers.getAdditionalInstanceField(activity, "mNeedGetDarkModeFromBackground");
			if (needGetDarkModeFromBackground) {
				Bitmap bitmap = Utils.getBitMapFromActivityBackground(activity);
				if (bitmap != null) {
					BitMapColor bitmapColor= Utils.getBitmapColor(bitmap);
					int color = bitmapColor.Color;
					boolean darkMode = Utils.getDarkMode(color);
					Intent intent = new Intent(Constant.INTENT_CHANGE_STATUSBAR_COLOR);
					intent.putExtra(Constant.STATUSBAR_BACKGROUND_COLOR, Color.TRANSPARENT);
					intent.putExtra(Constant.IS_DARKMODE, darkMode);
					intent.putExtra(Constant.DARKMODE_HANDLE, true);

					activity.sendBroadcast(intent);
				}
			}
			break;
		default:
			break;
		}

	}
}
