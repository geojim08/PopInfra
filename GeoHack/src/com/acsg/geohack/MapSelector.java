package com.acsg.geohack;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.acsg.Map.Map_PopulateBalloon;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

public class MapSelector extends MapActivity  {

    // Object for map
	private MapView myMapView = null;
    private Map_PopulateBalloon populate = null;
    
    private float fAccuracyM = 0;

	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.map_selector);
				   				
    	// Get setup type.
		Bundle extras = getIntent().getExtras();
		if (extras == null)
			return;
		
		boolean boAllowTap = extras.getBoolean("boAllowTap", false);
		boolean boIsDistance = extras.getBoolean("boIsDistance", false);
		double dLatDeg = extras.getDouble("dLatitude", 0);
		double dLonDeg = extras.getDouble("dLongitude", 0);
		this.fAccuracyM = extras.getFloat("fAccuracy", 0);
		
		TextView lblMapInstruction = (TextView)findViewById(R.id.lblMapInstruction);
		lblMapInstruction.setTextColor(Color.BLACK);
		
		if (boAllowTap)
			lblMapInstruction.setText("Localiser l'élément sur la carte...");
		else if (boIsDistance)
			lblMapInstruction.setText("Confirmer la position estimée...");
		else
			lblMapInstruction.setText("Confirmer la position GPS...");
			
		if (this.fAccuracyM != 0)
		{
			TextView lblMapPointInfo = (TextView)findViewById(R.id.lblMapPointInfo);
			lblMapPointInfo.setTextColor(Color.BLACK);
			lblMapPointInfo.setText(String.format(
				Locale.ENGLISH, "Position accuracy: %.1f m", this.fAccuracyM));
		}
		
		populate = new Map_PopulateBalloon(this, dLatDeg, dLonDeg, boAllowTap);

		// We always start with a point:
		// -Distance: result of the computation (can't move)
		// -GPS: From the GPS (can't move)
		// -Manual: From the GPS (can move)
		populate.vAddPoint(dLatDeg, dLonDeg);
				
		// Set Google view to 'Map'.
		myMapView = (MapView) findViewById(R.id.mapview);
		myMapView.setSatellite(false);		
		myMapView.setBuiltInZoomControls(false);					
    }	
	
    @Override
    protected boolean isRouteDisplayed()
    {
    	// Required by MapActivity.
        return false;
    }	
        
    
	public void onClickCancel(View theButton)
	{
    	Intent intent = new Intent(this, MapSelector.class);
        setResult(Activity.RESULT_CANCELED, intent);
		this.finish();
	}	

	public void onClickConfirm(View theButton)
	{		
    	GeoPoint gp = populate.GetCurrentPosition();
    	Intent intent = new Intent(this, MapSelector.class);
    	
    	if (gp == null)
    	{
            setResult(Activity.RESULT_CANCELED, intent);
    	}    		
    	else
    	{
	    	intent.putExtra("dLatitude", gp.getLatitudeE6() / 1e6);
	    	intent.putExtra("dLongitude", gp.getLongitudeE6() / 1e6);
	    	intent.putExtra("fAccuracy", this.fAccuracyM);
		
	    	setResult(Activity.RESULT_OK, intent);
    	}
		this.finish();
	}	
	
}
