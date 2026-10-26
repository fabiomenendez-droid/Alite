package de.phbouillon.android.games.alite;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public class AndroidUtil {
	private AndroidUtil() {		
	}
	
	public static void setImmersion(View view) {
		if (view == null) return;
		if (Build.VERSION.SDK_INT >= 30) {
			WindowInsetsController controller = view.getWindowInsetsController();
			if (controller != null) {
				controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
				if (Settings.navButtonsVisible) {
					controller.show(WindowInsets.Type.navigationBars());
					controller.hide(WindowInsets.Type.statusBars());
				} else {
					controller.hide(WindowInsets.Type.systemBars());
				}
			}
		} else {
			int flags = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
			if (!Settings.navButtonsVisible) {
				flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			}
			view.setSystemUiVisibility(flags);
		}
	}

	/** Keep the rendering surface and its local touch coordinates clear of cutouts. */
	public static void applySafeInsets(View container) {
		container.setOnApplyWindowInsetsListener((view, insets) -> {
			int left = 0, top = 0, right = 0, bottom = 0;
			if (Build.VERSION.SDK_INT >= 30) {
				int types = WindowInsets.Type.displayCutout();
				if (Settings.navButtonsVisible) types |= WindowInsets.Type.navigationBars();
				android.graphics.Insets safe = insets.getInsetsIgnoringVisibility(types);
				left = safe.left; top = safe.top; right = safe.right; bottom = safe.bottom;
			} else {
				if (Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
					android.view.DisplayCutout cutout = insets.getDisplayCutout();
					left = cutout.getSafeInsetLeft(); top = cutout.getSafeInsetTop();
					right = cutout.getSafeInsetRight(); bottom = cutout.getSafeInsetBottom();
				}
				if (Settings.navButtonsVisible) {
					left = Math.max(left, insets.getSystemWindowInsetLeft());
					right = Math.max(right, insets.getSystemWindowInsetRight());
					bottom = Math.max(bottom, insets.getSystemWindowInsetBottom());
				}
			}
			view.setPadding(left, top, right, bottom);
			return insets;
		});
		container.requestApplyInsets();
	}

	@SuppressLint("NewApi")
	public static void getDisplaySize(Point result, Display display) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !Settings.navButtonsVisible) {
			DisplayMetrics metrics = new DisplayMetrics();
			display.getRealMetrics(metrics);
			result.x = metrics.widthPixels;
			result.y = metrics.heightPixels;
		} else {
			DisplayMetrics metrics = new DisplayMetrics();
			display.getMetrics(metrics);
			result.x = metrics.widthPixels;
			result.y = metrics.heightPixels;
		}
	}
}
