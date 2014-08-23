package com.mohammadag.colouredstatusbar.hooks;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mohammadag.colouredstatusbar.ColourChangerMod;
import com.mohammadag.colouredstatusbar.MiuiV5Support;

import de.robv.android.xposed.callbacks.XC_LayoutInflated;

/* Taken from GravityBox */
public class StatusBarLayoutInflationHook extends XC_LayoutInflated {

	private ColourChangerMod mInstance;

	public StatusBarLayoutInflationHook(ColourChangerMod instance) {
		mInstance = instance;
	}

	@Override
	public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
		ViewGroup mSbContents = (ViewGroup) liparam.view.findViewById(liparam.res.getIdentifier(
				"status_bar_contents", "id", "com.android.systemui"));

		ViewGroup mIconArea = (ViewGroup) liparam.view.findViewById(
				liparam.res.getIdentifier("system_icon_area", "id", "com.android.systemui"));
		// for miuiv5
		if (MiuiV5Support.IS_MIUIV5) {
			mIconArea = (ViewGroup) liparam.view.findViewById(
					liparam.res.getIdentifier("icons", "id", "com.android.systemui"));
			// find miuiv5 carrier label
			TextView carrier = (TextView) mIconArea.findViewById(
					liparam.res.getIdentifier("carrier", "id", "com.android.systemui"));
			// find miuiv5 network speed
			TextView ntwspeed = (TextView) mIconArea.findViewById(
					liparam.res.getIdentifier("network_speed_view", "id", "com.android.systemui"));
			// add carrier label to TextLabel Array
			if (carrier != null) {
				mInstance.addTextLabel(carrier);
			}
			if (ntwspeed != null) {
				mInstance.addTextLabel(ntwspeed);
			}
		}
		
		// find statusbar clock
		TextView clock = (TextView) mIconArea.findViewById(
				liparam.res.getIdentifier("clock", "id", "com.android.systemui"));
		// the second attempt
		if (clock == null) {
			clock = (TextView) mSbContents.findViewById(
					liparam.res.getIdentifier("clock", "id", "com.android.systemui"));
		}
		
		try {
			ViewGroup centerClockLayout = (ViewGroup) liparam.view.findViewById(
					liparam.res.getIdentifier("center_clock_layout", "id", "com.android.systemui"));

			if (centerClockLayout != null) {
				TextView centerClock = (TextView) centerClockLayout.findViewById(
						liparam.res.getIdentifier("center_clock", "id", "com.android.systemui"));
				if (centerClock != null)
					mInstance.addTextLabel(centerClock);
			}
		} catch (Throwable t) {
			// No sense in logging this, it'll happen on most ROMs
		}


		if (clock != null) {
			mInstance.setClockFound();
			mInstance.addTextLabel(clock);
		} else {
			mInstance.setNoClockFound();
		}
		
		// if miuiv5, find and remove simple_statusbar from trackview
		if (MiuiV5Support.IS_MIUIV5) {
			ViewGroup mTrackViewGroup = (ViewGroup) liparam.view.findViewById(liparam.res.getIdentifier(
					"expanded_fixed", "id", "com.android.systemui"));
			ViewGroup mSimpleStatusBar = (ViewGroup) mTrackViewGroup.findViewById(liparam.res.getIdentifier(
					"status_bar_simple", "id", "com.android.systemui"));
			if (mSimpleStatusBar != null) {
				mSimpleStatusBar.setVisibility(View.GONE);
			}
		}
	}
}
