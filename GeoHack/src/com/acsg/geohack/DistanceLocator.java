package com.acsg.geohack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.acsg.geohack.DistanceLSQ.CoordinateStruct;

public class DistanceLocator extends Activity implements SurfaceHolder.Callback, SensorEventListener  
{
	private final static String stcstrCaptureMessageInitial = 
			"Viser la base de l'élément...";
	private final static String stcstrCaptureMessageCounter = 
			"Dernière distance mesurée (%d):\n%.1f m";
	private final static double stcdDefaultUserHeight = 1.62;
	private final static int stciMaximumAccuracyM = 25;
	
	private SurfaceView mSurfaceView = null;
	private SurfaceHolder mSurfaceHolder = null;
	private Camera mCamera = null;
	private boolean mPreviewRunning = false;
	private Camera.Parameters mCameraParam = null;
	private int iCurrentZoomLevel = -1;
	private int iMaxZoomLevel = -1;
	private DrawOnTop mDraw = null;
	
	private TextView lblVAngle = null;
	private TextView lblGPSStatus = null;
	private TextView lblCapture = null;
	private Button cmdCompute = null;
	private Button cmdReset = null;
	
	private MyLocationListener GPSListener = null;
	private LocationManager locationManager = null;
	
	private Sensor mAccelerometer = null;  
	private Sensor mMagneticField = null;
	private SensorManager mSensorManager = null;
    private float[] afAccValues = null;  
    private float[] afMagFieldValues = null; 
    private float fVerticalAngle = 0;
    
	private double dLastLatDeg = 0;
	private double dLastLonDeg = 0;
	private float fLastAccuracyM = 0;
    	    
	private DistanceLSQ mCalculator = null;
	
	private float[] afVAngleBuffer = null;
	private int iVAngleBufferIndex = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.distance_locator);

		mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		
		if (Build.VERSION.SDK_INT <= 10) 
			mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        lblCapture = (TextView)findViewById(R.id.lblCapture);
        lblCapture.setTextColor(Color.GREEN);
		lblCapture.setText(stcstrCaptureMessageInitial);

        lblGPSStatus = (TextView)findViewById(R.id.lblGPSStatus);
        lblGPSStatus.setTextColor(Color.GREEN);

        lblVAngle = (TextView)findViewById(R.id.lblVAngle);
        lblVAngle.setTextColor(Color.GREEN);
        
        cmdCompute = (Button)findViewById(R.id.btnCompute);
        cmdCompute.setEnabled(false);
        cmdReset = (Button)findViewById(R.id.btnReset);
        cmdReset.setEnabled(false);
        
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); 
		mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		mCalculator = new DistanceLSQ();
		
		afVAngleBuffer = new float[5];
		
		GPSListener = new MyLocationListener();
		locationManager = (LocationManager)getSystemService(
				Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
				1000, 0, GPSListener);			
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		return true;
	}

    @Override
    public void onResume()
    {
    	super.onResume();  
    	
    	mSensorManager.registerListener(this, mAccelerometer, 
    			SensorManager.SENSOR_DELAY_UI);    
    	mSensorManager.registerListener(this, mMagneticField, 
    			SensorManager.SENSOR_DELAY_UI);      	
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
        		1000, 0, GPSListener);    	
    }
    
    
	@Override
	public void onPause() 
	{
		super.onPause();		
		mSensorManager.unregisterListener(this);		
	    locationManager.removeUpdates(GPSListener);		
	}    
    
	
    public void onAccuracyChanged(Sensor sensor, int accuracy) 
    {
    	// can be safely ignored for this demo
    }
    
    public void onSensorChanged(SensorEvent event) 
    {    	
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        	return;         
    	
    	// Handle the events for which we registered
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
        	afAccValues = event.values.clone();
        	if (afAccValues != null && mDraw != null)
        	{
        		// Set color according the device's verticality.
        		int iColor = Color.RED;
        		if (afAccValues[1] > -9 && afAccValues[0] < 1 && afAccValues[0] > -1)
        			iColor = Color.GREEN;
        		mDraw.vSetColor(iColor);
                lblVAngle.setTextColor(iColor);
        	}
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)      
        	afMagFieldValues = event.values.clone();    

    	if (afAccValues != null && afMagFieldValues != null) 
    	{      
    		float R1[] = new float[9];      
    		if (SensorManager.getRotationMatrix(R1, null, afAccValues, afMagFieldValues))
    		{
    			float orientation[] = new float[3];
    			SensorManager.getOrientation(R1, orientation);

    			// final double cdRadToDeg = 180.0 / Math.PI;
    			// double d1 = orientation[0] * cdRadToDeg;
    			// double d2 = orientation[1] * cdRadToDeg;
    			// double d3 = orientation[2] * cdRadToDeg;
    			
    			// if (d1 < 0)
    			//	d1 += 360.0;
    	
    			this.fVerticalAngle = Math.abs(orientation[1]);

    			this.afVAngleBuffer[this.iVAngleBufferIndex++] = this.fVerticalAngle;
    			if (this.iVAngleBufferIndex == this.afVAngleBuffer.length)
    				this.iVAngleBufferIndex = 0;
    			
    			this.lblVAngle.setText(
    					String.format("Angle V: %.1f°", this.fVerticalAngle * 180.0 / Math.PI));
    		}
        }
    }
    
    
    public void onClickCompute(View v)
    {
    	// Compute location using the positions and distances.
        CoordinateStruct sResult = null;
        
    	try
    	{    		
    		if (mCalculator.iGetNbObs() > 1 && mCalculator.boCompute())
    			sResult = mCalculator.GetResult();    		
    	}catch(Exception ex)
    	{}
    	
    	if (sResult == null)
    	{
    		Toast toast = Toast.makeText(getBaseContext(), 
    				"L'ajustement GPS/Distance a produit une erreur...\n" +
    				"Prendre une mesure supplémentaire!", 
            		Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            return;
    	}
    	
    	// Start map selector to confirm position
		Intent i = new Intent(getApplicationContext(), 
				MapSelector.class);
		i.putExtra("dLatitude", sResult.dLat); 
		i.putExtra("dLongitude", sResult.dLon);
		i.putExtra("fAccuracy", sResult.fAccuracy);
		i.putExtra("boAllowTap", false); 
		i.putExtra("boIsDistance", true);
		
        startActivityForResult(i, MainActivity.ciRequestID_Map);        	
    }
    
    public void onClickCapture(View v)
    {	
    	// Get the median value of the cumulated array.
    	float[] afSort = new float[this.afVAngleBuffer.length];
    	System.arraycopy(this.afVAngleBuffer, 0, afSort, 0, this.afVAngleBuffer.length);    	
    	Arrays.sort(afSort);
    	float fVAngle = afSort[afSort.length / 2];
    	
    	double dDistM = stcdDefaultUserHeight * Math.tan(this.fVerticalAngle);
    	
    	String strMessage = String.format(Locale.ENGLISH, "Distance estimée: %.1f m.", dDistM);
    	if (this.fLastAccuracyM == 0)
    		strMessage += "\nAucune position GPS disponible...";
    	else if (this.fLastAccuracyM > stciMaximumAccuracyM)
    	{
    		strMessage += String.format(Locale.ENGLISH, 
    				"\nLa précision GPS doit être d'au moins %d m...", 
    				stciMaximumAccuracyM);
    	}
    	
		Toast toast = Toast.makeText(getBaseContext(), strMessage, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();

        if (this.fLastAccuracyM == 0 || this.fLastAccuracyM > stciMaximumAccuracyM)
            return;    	    		
    	
        this.mCalculator.vAddObs(this.dLastLatDeg, this.dLastLonDeg, this.fLastAccuracyM, dDistM);
    	
		String str = "";
		str += String.valueOf(this.mCalculator.iGetNbObs()) + ",";
		str += String.valueOf(this.dLastLatDeg) + ",";
		str += String.valueOf(this.dLastLonDeg) + ",";
		str += String.valueOf(this.fLastAccuracyM) + ",";
		str += String.valueOf(dDistM) + "\n";
			
		try {
			 String strFile = Environment.getExternalStorageDirectory() + File.separator + "Effigis" + File.separator + "LSQ.csv";
			 FileOutputStream fos = new FileOutputStream(strFile, true);
			 OutputStreamWriter osw = new OutputStreamWriter(fos);
		     osw.write(str);
		     osw.close();
		     fos.close();
		    
		}catch(Exception ex){}
    	
    	cmdCompute.setEnabled(mCalculator.iGetNbObs() > 1);
        cmdReset.setEnabled(true);
        
		lblCapture.setText(String.format(Locale.ENGLISH, 
				stcstrCaptureMessageCounter, 
				mCalculator.iGetNbObs(), dDistM));        
    }

    public void onClickReset(View v)
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Attention!");
	
		builder.setMessage("Êtes-vous certain de vouloir réinitialiser le processus de saisi?");
		
		builder.setPositiveButton("Oui", 
				new DialogInterface.OnClickListener() 
		{
            public void onClick(DialogInterface dialog, int which) 
            {
            	mCalculator.vReset();
            	cmdCompute.setEnabled(false);
            	cmdReset.setEnabled(false);
            }
        });
		
        builder.setNegativeButton("Non", null);     	        
        builder.show();    	
    	
		lblCapture.setText(stcstrCaptureMessageInitial);
    }
    
    public void onClickZoomIn(View v)
    {
    	if (iCurrentZoomLevel < iMaxZoomLevel)
    	{
    		iCurrentZoomLevel += 5;
    		mCameraParam.setZoom(iCurrentZoomLevel);
    		mCamera.setParameters(mCameraParam);
    	}
    }

    public void onClickZoomOut(View v)
    {
    	if (iCurrentZoomLevel > 0)
    	{
    		iCurrentZoomLevel -= 5;
    		mCameraParam.setZoom(iCurrentZoomLevel);
    		mCamera.setParameters(mCameraParam);
    	}
    }
    
	public void surfaceCreated(SurfaceHolder holder) {

		if (mDraw == null)
		{
			LinearLayout layout = (LinearLayout)findViewById(R.id.layout_button);
			Point size = new Point();
			vGetSize(size);
			
			int screenCenterX = (size.x / 2);
			int screenCenterY = (size.y / 2) - layout.getHeight();		
			mDraw = new DrawOnTop(this,screenCenterX, screenCenterY); 
			addContentView(mDraw, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)); 				
		}
		
		// mCamera is an Object of the class “Camera”. In the surfaceCreated 
		// we “open” the camera. This is how to start it!!
		mCamera = Camera.open();

	}
	
	@TargetApi(13)
	public void vGetSize(Point size) 
	{
		Display display = getWindowManager().getDefaultDisplay();
		if (Build.VERSION.SDK_INT >= 13)
			display.getSize(size);
		else
		{
	      size.x = display.getWidth();
	      size.y = display.getHeight();
		}		
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

		if (mCamera == null)
			return;
		
		if (mPreviewRunning)
			mCamera.stopPreview();
		
		mCameraParam = mCamera.getParameters();
		
		if (iMaxZoomLevel == -1)
		{
			iMaxZoomLevel = mCameraParam.getMaxZoom();
			iCurrentZoomLevel = mCameraParam.getZoom();
		}
		
		// Get the highest resolution for preview.
		Size sizeForPreview = null;
		List<Size> sizes = mCameraParam.getSupportedPreviewSizes();		
		for (int i = 0; i < sizes.size(); i++)
		{
			Size s = sizes.get(i);
			if (sizeForPreview == null || s.height > sizeForPreview.height)
				sizeForPreview = s; 
		}
		
		Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if(display.getRotation() == Surface.ROTATION_0)
            mCamera.setDisplayOrientation(90);
        else if(display.getRotation() == Surface.ROTATION_270)
            mCamera.setDisplayOrientation(180);
        
		try 
		{
			if (sizeForPreview != null)
				mCameraParam.setPreviewSize(sizeForPreview.width, sizeForPreview.height); 
			
			mCamera.setParameters(mCameraParam);
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (Exception e) {
			e.printStackTrace();
		}

		mPreviewRunning = true;
	}
		
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		mPreviewRunning = false;
		if (mCamera != null)
		{
			mCamera.stopPreview();
			mCamera.release();
		}
	}	
	
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == MainActivity.ciRequestID_Map)
        {
	    	Intent intent = new Intent(this, DistanceLocator.class);
            if (resultCode == Activity.RESULT_OK) 
            {	            	
            	double dLat = data.getExtras().getDouble("dLatitude", 0);        			
            	double dLon = data.getExtras().getDouble("dLongitude", 0); 
            	float fAccuracy = data.getExtras().getFloat("fAccuracy", 0);
            	
    	    	intent.putExtra("dLatitude", dLat);
    	    	intent.putExtra("dLongitude", dLon);
    	    	intent.putExtra("fAccuracy", fAccuracy);
            }

            // Finish activity
	        setResult(resultCode, intent);
	        this.finish();
        }
    }
	

    /*
     * Class to draw the cross-hair symbol.
    */
    private class DrawOnTop extends View 
    { 
    	private int screenCenterX = 0;
        private int screenCenterY = 0;
        private int iColor = Color.RED;
        private Paint p = new Paint();
                
		public DrawOnTop(Context context, int screenCenterX, int screenCenterY) 
		{ 
			super(context); 
		    this.screenCenterX = screenCenterX;
		    this.screenCenterY = screenCenterY;
		} 
		
		public void vSetColor(int iColor)
		{
	         this.iColor = iColor;			
		}

	    @Override 
	    protected void onDraw(Canvas canvas) 
	    { 
	         p.setColor(this.iColor);
	         p.setStrokeWidth(5);	         
	         canvas.drawLine(0, screenCenterY, 
	        		 2 * screenCenterX, screenCenterY, p);
//	         canvas.drawLine(screenCenterX, screenCenterY - iOffset, 
//	        		 screenCenterX, screenCenterY + iOffset, p);
	         	         
	         invalidate();
	         super.onDraw(canvas); 
	    } 
    }	
    
	/*----------Listener class to get coordinates ------------- */
	private class MyLocationListener implements LocationListener 
	{
	    @Override
	    public void onLocationChanged(Location loc) 
	    {	    	
	    	dLastLatDeg = loc.getLatitude();
	    	dLastLonDeg = loc.getLongitude();
	    	fLastAccuracyM = loc.getAccuracy();
	    	lblGPSStatus.setText(String.format(
	    			Locale.ENGLISH, "Précision GPS: %.1f m", fLastAccuracyM));
	    }

	    @Override
	    public void onProviderDisabled(String provider) {}

	    @Override
	    public void onProviderEnabled(String provider) {}

	    @Override
	    public void onStatusChanged(String provider, int status, Bundle extras) {}
	}
    
}
