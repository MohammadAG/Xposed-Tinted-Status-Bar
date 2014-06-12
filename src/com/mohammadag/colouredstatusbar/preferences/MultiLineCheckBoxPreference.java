package com.mohammadag.colouredstatusbar.preferences;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MultiLineCheckBoxPreference extends CheckBoxPreference{

	public MultiLineCheckBoxPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onBindView( View view){
	    super.onBindView(view);
	    makeMultiline(view);  	 
	}

	 protected void makeMultiline( View view) {
		 if (view instanceof ViewGroup)
		 {
			 ViewGroup viewGroup=(ViewGroup)view;

			 for (int index = 0; index < viewGroup.getChildCount(); index++)
			 {
				 makeMultiline(viewGroup.getChildAt(index));
			 }
		 } else if (view instanceof TextView)
		 {
		        TextView t = (TextView)view;
		        t.setSingleLine(false); 
		        t.setEllipsize(null);
		 }
	}
}
