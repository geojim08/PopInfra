package com.acsg.Map;

import java.util.List;

import android.app.Activity;
import android.content.res.Resources;

import com.acsg.Conversion.GeodeticStruct;
import com.acsg.geohack.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;


public class Map_PopulateBalloon { 

	// Zoom is from 1 to 21.	
	private static final int stciInitialZoomValue = 20;			
	private static final int stciDefaultZoomValue = 5;			

	private static final double dDefaultLatDeg = 45.865150;
	private static final double dDefaultLonDeg = -73.224986;
	
	private MapView myMapView;
	private MapController mc = null;
	private List<Overlay> mapOverlays;
	private Map_ItemizedOverlay PointOverlay = null;
			
	public Map_PopulateBalloon(Activity currentActivity, double dIniLatDeg, 
			double dIniLonDeg, boolean boAllowTap)
	{		
    	// Get MapView
    	myMapView = (MapView)currentActivity.findViewById(R.id.mapview);
    	
    	// Get controller
		mc = myMapView.getController();

		// Get Overlays
		mapOverlays = myMapView.getOverlays();		

		Resources res = currentActivity.getResources();
		PointOverlay = new Map_ItemizedOverlay(
				res.getDrawable(R.drawable.balloon_circle_green), 
				myMapView, boAllowTap);	
				
		if (dIniLatDeg != 0 && dIniLonDeg != 0)
		{
			mc.animateTo(stDoubleToGeoPoint(dIniLatDeg, dIniLonDeg));
			mc.setZoom(stciInitialZoomValue);
		}
		else
		{
			mc.animateTo(stDoubleToGeoPoint(dDefaultLatDeg, dDefaultLonDeg));
			mc.setZoom(stciDefaultZoomValue);
		}
	}

	
	public void vAddPoint(double dLatDeg, double dLonDeg)
	{
		mapOverlays.clear();						
		
		GeoPoint point = null;
		
		if (dLatDeg != -100)
			point = stDoubleToGeoPoint(dLatDeg, dLonDeg);
		else
			point = stDoubleToGeoPoint(dDefaultLatDeg, dDefaultLonDeg);
			
		mc.animateTo(point);
		
		GeodeticStruct sPos = new GeodeticStruct(dLatDeg, dLonDeg, 0, true);

		String strLat = "";
		if (sPos.cLatHemis == GeodeticStruct.stccSouth)
			strLat += "-";
		strLat += String.format("%02d°%02d'%06.3f\" ", 
				sPos.iLatDeg, sPos.iLatMin, sPos.dLatSec);
		
		String strLon = "";
		if (sPos.cLonHemis == GeodeticStruct.stccWest)
			strLon += "-";
			strLon += String.format("%d°%02d'%06.3f\" ", 
					sPos.iLonDeg, sPos.iLonMin, sPos.dLonSec);
		  					
		OverlayItem overlayItem = new OverlayItem(point, "Collected Feature",
				strLat + "\n" + strLon);

		PointOverlay.addOverlay(overlayItem);				 			 
		
		// add to the map
		mapOverlays.add(PointOverlay);		
		
		// Update map view.
		myMapView.invalidate();				
	}
	
	public GeoPoint GetCurrentPosition()
	{
		if (PointOverlay.size() == 0)
			return null;
		
		return PointOverlay.GetGeoPoint(0);
	}
	
	    			
	private static GeoPoint stDoubleToGeoPoint(double dLat, double dLon)
	{
		//Convert double to GeoPoint 	
		// watch out! For GeoPoint, first:latitude, second:longitude	
		return new GeoPoint((int) (dLat * 1E6),(int) (dLon * 1E6));
	}
	
}
