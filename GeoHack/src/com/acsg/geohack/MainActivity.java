package com.acsg.geohack;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String stcstrURL =
		"http://192.168.1.109:8080/apps/Carto.html?lat=%.5f&lon=%.5f&zoom=19";
		//"http://sigp.effigis.com/SGIP-GEO/PopInfra/Carto.html?lat=%.5f&lon=%.5f&zoom=15"; 
		//"http://www.openstreetmap.org/#map=17/%12.8f/%12.8f";
	
	private static final CharSequence[] acsPositionMode = {" GPS "," Carte "," GPS+Distance "};	
	
    public static final int ciRequestID_Map = 1000;
    public static final int ciRequestID_Distance = 1001;
    public static final int ciRequestID_Describe = 1002;
	
	private AlertDialog PositionModeDialog = null;
	
	private MyLocationListener GPSLocationListener = null;
	private MyLocationListener NetworkLocationListener = null;
	private LocationManager locationManager = null;
	
	// private ProgressDialog ProcessDialog = null;
	
	private double dFeatureLat = 0;
	private double dFeatureLon = 0;
	private float fFeatureAccuracy = 0;
	private String strUserID = "";        			
	private String strFeatureType = ""; 
	private String strFeatureSection = "";
	
	private Button cmdLocalize = null;
	private Button cmdDescribe = null;
	private Button cmdBroadcast = null;
		
	private double dLastLatDeg = 0;
	private double dLastLonDeg = 0;
	private float fLastAccuracyM = 0;
	
	
	private WebView webView = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		cmdLocalize = (Button) findViewById(R.id.btnLocalize);
		cmdLocalize.setEnabled(true);
		cmdDescribe = (Button) findViewById(R.id.btnDescribe);
		cmdDescribe.setEnabled(false);
		cmdBroadcast = (Button) findViewById(R.id.btnBroadcast);
		cmdBroadcast.setEnabled(false);
		
		webView = (WebView) findViewById(R.id.webView1);
		webView.getSettings().setJavaScriptEnabled(true);
		
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			    
		vGetCoarseLocationAndSetUpdates();
	    	    
				
    	//ProcessDialog = ProgressDialog.show(this, "GPS Connection", 
    	//		"Waiting for a fix solution...", true);	
/*		
		DistanceLSQ dLSQ = new DistanceLSQ();
		VdMat vdV = null;
		try
		{
// Test 1
//			dLSQ.vAddObs(9.520, 17.565, 1.0F, 486.6);
//			dLSQ.vAddObs(376.095, 99.825, 1.0F, 552.7);
//			dLSQ.vAddObs(403.259, 465.951, 1.0F, 405.9);
//			dLSQ.vAddObs(280.118, 661.701, 1.0F, 322.2);

			
// Test 2
			// Set 1: OK			
			//dLSQ.vAddObs(45.86519282, -73.22455689, 25, 4.08541805);
			//dLSQ.vAddObs(45.86523545, -73.22460132, 20, 3.558933253);
			//dLSQ.vAddObs(45.86521596, -73.22460023, 15, 4.185020217);

			// Set 2: no convergence? GDOP=1.2
			dLSQ.vAddObs(45.86525854, -73.22468354, 25, 4.076282459);
			dLSQ.vAddObs(45.86518294, -73.22461557, 20, 3.321995688);
			dLSQ.vAddObs(45.86517887, -73.22459939, 20, 4.400838486);

			// Set 3: OK
			//dLSQ.vAddObs(45.86526688, -73.22461934, 20, 3.962340865);
			//dLSQ.vAddObs(45.86526307, -73.22466284, 20, 3.575994036);
			//dLSQ.vAddObs(45.86524371, -73.22464122, 15, 4.162157885);

			// Set 4: OK GDOP=1.46
			//dLSQ.vAddObs(45.86522878613323,-73.22466845624149,5,10.11996706501117);
			//dLSQ.vAddObs(45.86520079057664,-73.22464012540877,10,7.754260421379553);
			//dLSQ.vAddObs(45.86521721910685,-73.22475210763514,10,7.704069422472868);
			
			dLSQ.boCompute();			
			vdV = dLSQ.vdGetResiduals();
	
		}catch(Exception ex){}	
		
		CoordinateStruct s = dLSQ.GetResult();
		
//		double dN = 9.520 - s.dLat;
//		double dE = 17.565 - s.dLon;
//		double dR1 = Math.sqrt(dN*dN + dE*dE);
//		dN = 376.095 - s.dLat;
//		dE = 99.825 - s.dLon;
//		double dR2 = Math.sqrt(dN*dN + dE*dE);
*/			
	}
		
	private void vRefreshWebPage(int iType, double dLatDeg, double dLonDeg, float fAccuracyM)
	{
		if (iType == MyLocationListener.stciTypeGPS && NetworkLocationListener != null)
	    {
	    	locationManager.removeUpdates(NetworkLocationListener);
	    	NetworkLocationListener = null;
	    }
					
		this.dLastLatDeg = dLatDeg;
		this.dLastLonDeg = dLonDeg;
		this.fLastAccuracyM = fAccuracyM;
		
		String str = String.format(Locale.ENGLISH, stcstrURL, dLatDeg, dLonDeg);
		this.webView.loadUrl(str);		
	}
	
	
	private void vGetCoarseLocationAndSetUpdates()
	{
		// Get a coarse coordinate to start.
	    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
	    {
		    Location lastLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);	    
		    if (lastLoc != null) 
		    {
		    	this.dLastLatDeg = lastLoc.getLatitude();
		    	this.dLastLonDeg = lastLoc.getLongitude();
		    	this.fLastAccuracyM = 0;
		    	vRefreshWebPage(MyLocationListener.stciTypeNetwork, 
		    			this.dLastLatDeg, this.dLastLonDeg, this.fLastAccuracyM);
		    }
	    }

	    // Register to the NETWORK to get updates.    
	    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && 
	    		NetworkLocationListener == null) 
	    {
	    	NetworkLocationListener = new MyLocationListener(
	    			MyLocationListener.stciTypeNetwork);
	        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
	        		1000, 5, NetworkLocationListener);
	    } 	    
	    
	    // Register to the GPS to get updates.    
	    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
	    		GPSLocationListener == null) 
	    {
	    	GPSLocationListener = new MyLocationListener(
	    			MyLocationListener.stciTypeGPS);
	        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
	        		1000, 5, GPSLocationListener);
	    } 	    
	}	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
    @Override
    public void onResume() 
    {
        super.onResume();        
        vGetCoarseLocationAndSetUpdates();
    }
    
	@Override
	public void onPause() 
	{
	    super.onPause();
	    if (GPSLocationListener != null)
	    {
	    	locationManager.removeUpdates(GPSLocationListener);
	    	GPSLocationListener = null;
	    }
	    
	    if (NetworkLocationListener != null)
	    {
	    	locationManager.removeUpdates(NetworkLocationListener);
	    	NetworkLocationListener = null;
	    }
	}	
		
	public void onClickLocalize(View theButton)
	{				
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Selectionner le mode de positionnement");
		builder.setSingleChoiceItems(acsPositionMode, -1, new DialogInterface.OnClickListener() 
		{
            public void onClick(DialogInterface dialog, int item) 
            {                               
                switch(item)
                {
                	case 0:
                    case 1:
                	{
                		if (dLastLatDeg == 0 && dLastLonDeg == 0)
                		{
                	        Toast.makeText(getBaseContext(), "Pas de position GPS valide pour initialiser la carte...", 
                	        		Toast.LENGTH_SHORT).show();                				
                		}
                		
                    	// Start map selector to confirm position
                		Intent i = new Intent(getApplicationContext(), 
                				MapSelector.class);
        				i.putExtra("dLatitude", dLastLatDeg); 
        				i.putExtra("dLongitude", dLastLonDeg);
        				i.putExtra("fAccuracy", fLastAccuracyM);
        				i.putExtra("boAllowTap", (item == 1)); 
                        startActivityForResult(i, ciRequestID_Map);        	
                        break;
                	}
                    case 2:
                    {
                    	// GPS + Distance positioning mode.
                		Intent i = new Intent(getApplicationContext(), 
                				DistanceLocator.class);
                        startActivityForResult(i, ciRequestID_Distance);                    	
                        break;
                    }
                }
                PositionModeDialog.dismiss();
            }
        });		
		
		PositionModeDialog = builder.create();
		PositionModeDialog.show();    			
	}	

	
	public void onClickDescribe(View theButton)
	{
		Intent i = new Intent(getApplicationContext(), Describe.class);
		i.putExtra("dFeatureLat", dFeatureLat); 
		i.putExtra("dFeatureLon", dFeatureLon); 
        startActivityForResult(i, ciRequestID_Describe);        	
	}	
	
	
	public void onClickBroadcast(View theButton)
	{		
        AsyncCallWS task = new AsyncCallWS();
        task.execute();		
	}
	
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
	        case ciRequestID_Map : 
	        case ciRequestID_Distance :
	        {
        		cmdDescribe.setEnabled(false);
        		cmdBroadcast.setEnabled(false);
	            if (resultCode == Activity.RESULT_OK) 
	            {	            	
	            	this.dFeatureLat = data.getExtras().getDouble("dLatitude", 0);        			
	            	this.dFeatureLon = data.getExtras().getDouble("dLongitude", 0); 
	            	this.fFeatureAccuracy = data.getExtras().getFloat("fAccuracy", 0);
	        		cmdDescribe.setEnabled(true);
	            }	        
	            break;
	        }
	        case ciRequestID_Describe :
	        {
	            if (resultCode == Activity.RESULT_OK) 
	            {	            
	            	strUserID = data.hasExtra("strUserID") ? 
	            			data.getStringExtra("strUserID") : "";
	            	strFeatureType = data.hasExtra("strFeatureType") ? 
	            			data.getStringExtra("strFeatureType") : "";
	            	strFeatureSection = data.hasExtra("strFeatureSection") ? 
	            			data.getStringExtra("strFeatureSection") : "";	            		
	        		cmdBroadcast.setEnabled(true);
	            }	        
	            break;
	        }
        }
    }
	
	
	/*----------Listener class to get coordinates ------------- */
	private class MyLocationListener implements LocationListener 
	{
		public final static int stciTypeGPS = 1;
		public final static int stciTypeNetwork = 2;
		
		private int iProviderType = 0;
		
		public MyLocationListener(int iType)
		{
			iProviderType = iType;
		}
		
	    @Override
	    public void onLocationChanged(Location loc) 
	    {	   	    	
	        vRefreshWebPage(iProviderType, loc.getLatitude(), loc.getLongitude(), loc.getAccuracy());
	    }

	    @Override
	    public void onProviderDisabled(String provider) {}

	    @Override
	    public void onProviderEnabled(String provider) {}

	    @Override
	    public void onStatusChanged(String provider, int status, Bundle extras) {}
	}
	
	private class AsyncCallWS extends AsyncTask<String, Void, Void> 
	{
		String strResult = "";
		
        @Override
        protected Void doInBackground(String... params) {
        	vPostDataToPopInfra();
            return null;
        }
 
        @Override
        protected void onPostExecute(Void result) 
        {
        	String str = "";
        	if (strResult.indexOf("SUCCESS") > -1)
        	{
        		str = "Inégration réussi!";
        		cmdBroadcast.setEnabled(false);
        		cmdDescribe.setEnabled(false);
        		vRefreshWebPage(MyLocationListener.stciTypeNetwork, dFeatureLat, dFeatureLon, 0);
        	}
        	else
        	{
        		 str = "Problème lors de l'envoi des données!";
        	}
        	
        	Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();        	
        }
 
        @Override
        protected void onPreExecute() {
        }
 
        @Override
        protected void onProgressUpdate(Void... values) {
        }
            
        private void vPostDataToPopInfra()
        {
            try {
            	
        		// Get UTC time.
        		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        		fFeatureAccuracy = 10;
        		strFeatureType = "BI";
            	
            	HttpClient httpClient = new DefaultHttpClient();
/*            	            	
            	// GetCapabilities
            	String strPost = 
            		"<?xml version=\"1.0\" ?>" +
            		"<GetCapabilities " +
            		"service=\"WFS\" " +
            	    "version=\"1.0.0\" " +
            	    "xmlns=\"http://www.opengis.net/wfs\" />";
*/            	           	
/*
        		// Update de tous les records	
            	String strPost = 
        			"<?xml version=\"1.0\"?>" +
        			"<wfs:Transaction " +
        				"version=\"1.0.0\" "+
        				"service=\"WFS\" " +
        				"xmlns:gml=\"http://www.opengis.net/gml\" " +
        				"xmlns:ogc=\"http://www.opengis.net/ogc\" " +
        				"xmlns:wfs=\"http://www.opengis.net/wfs\" " +
        				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >" +
        				"<wfs:Update typeName=\"PopInfra:Poteau\">" +
        					"<wfs:Property>" +
        			    		"<wfs:Name>ID_RELEVEU</wfs:Name>" +
        			    		"<wfs:Value>Germaine</wfs:Value>" +
        			    	"</wfs:Property>" +
        			    "</wfs:Update>" + 
        			"</wfs:Transaction>";
*/
/*            	
            	// Add Poteau
            	String strPost = 
        			"<?xml version=\"1.0\"?>" +
        			"<wfs:Transaction " +
        				"version=\"1.0.0\" "+
        				"service=\"WFS\" " +
        				"xmlns:gml=\"http://www.opengis.net/gml\" " +
        				"xmlns:ogc=\"http://www.opengis.net/ogc\" " +
        				"xmlns:wfs=\"http://www.opengis.net/wfs\" " +
        				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
        				"xmlns:popinfra=\"http://sigp.effigis.com/PopInfra\" >" +
        				"<wfs:Insert>" +
    						"<popinfra:Poteau>" +
    							"<the_geom>" +
    								"<gml:Point>" +
    									"<gml:coordinates>" +
    										"-74.0,46.0" +
    									"</gml:coordinates>" +
    								"</gml:Point>" +
    							"</the_geom>" + 
    							"<OBJECTID>999</OBJECTID>" +
    							"<ID_RELEVEU>Luco</ID_RELEVEU>" +
    							"<QA_NOTE>Jour du GeoHack</QA_NOTE>" +
    						"</popinfra:Poteau>" +
    					"</wfs:Insert>" +
        			"</wfs:Transaction>";
*/            	
            	
            	// Insert 
            	String strPost = 
        			"<?xml version=\"1.0\"?>" +
        			"<wfs:Transaction " +
        				"version=\"1.0.0\" "+
        				"service=\"WFS\" " +
        				"xmlns:gml=\"http://www.opengis.net/gml\" " +
        				"xmlns:ogc=\"http://www.opengis.net/ogc\" " +
        				"xmlns:wfs=\"http://www.opengis.net/wfs\" " +
        				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
        				//"xmlns:popinfra=\"http://sigp.effigis.com/PopInfra\" >" +
        				"xmlns:popinfra=\"localhost:8080/apps\" >" +
        				"<wfs:Insert>" +
    						"<popinfra:ELEM_P>" +
//    							"<ID_REGROUP>5017056</ID_REGROUP>" +
    							"<TYPE>" + strFeatureType + "</TYPE>" +
    						    "<ID_TRC>" + strFeatureSection + "</ID_TRC>" +
    							"<DATE_CREATION>" + sdf.format(new Date()) + "</DATE_CREATION>" +
    							"<ID_USER>" + strUserID + "</ID_USER>" +
    							"<PRECISION>" + String.format("%.3f", fFeatureAccuracy) + "</PRECISION>" +
//    							"<URL_PHOTO>Bla</URL_PHOTO>" +
    							"<geometry>" +
									"<gml:Point>" +
										"<gml:coordinates>" +
											String.format("%.8f", dFeatureLon) + "," + 
											String.format("%.8f", dFeatureLat) + 
										"</gml:coordinates>" +
									"</gml:Point>" +
								"</geometry>" + 
    						"</popinfra:ELEM_P>" +
    					"</wfs:Insert>" +
        			"</wfs:Transaction>";
       	            	            	            	
/*
            	// Delete
            	String strPost = 
        			"<?xml version=\"1.0\"?>" +
        			"<wfs:Transaction " +
        				"version=\"1.0.0\" "+
        				"service=\"WFS\" " +
        				"xmlns:gml=\"http://www.opengis.net/gml\" " +
        				"xmlns:ogc=\"http://www.opengis.net/ogc\" " +
        				"xmlns:wfs=\"http://www.opengis.net/wfs\" " +
        				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >" +            			
	            		"<wfs:Delete typeName=\"PopInfra:Poteau\">" +
	            			"<ogc:Filter>" +
	            	                "<ogc:PropertyIsEqualTo>" +
	            	                	"<ogc:PropertyName>OBJECTID</ogc:PropertyName>" +
	            	                    "<ogc:Literal>100</ogc:Literal>" +
	            	                "</ogc:PropertyIsEqualTo>" +
	            	        "</ogc:Filter>" +
	            	    "</wfs:Delete>" +            	
            		"</wfs:Transaction>";
*/
            	
            	// https://groups.google.com/forum/#!topic/android-query/NIh000PBU2M
            	// Create a new HttpClient and Post Header
            	HttpPost httppost = new HttpPost("http://192.168.1.109:8080/geoserver/popinfra/wms");//"http://sigp.effigis.com/geoserver/PopInfra/wfs");

            	StringEntity string_entity = new StringEntity(strPost, HTTP.UTF_8);
            	string_entity.setContentType("text/xml");
            	httppost.setEntity(string_entity);
            	BasicHttpResponse httpResponse = (BasicHttpResponse) httpClient.execute(httppost);
            	HttpEntity response_entity = httpResponse.getEntity();
            	strResult = EntityUtils.toString(response_entity);
            	strResult +="";
                                      
			} catch (ClientProtocolException cpe) {
				System.out.println("Exception generates caz of httpResponse :" + cpe);
				cpe.printStackTrace();
			} catch (IOException ioe) {
				System.out.println("Second exception generates caz of httpResponse :" + ioe);
				ioe.printStackTrace();
			}
        }        
    }		
	
		
}
