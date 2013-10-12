package com.acsg.geohack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class Describe extends Activity 
{
    private Spinner cboSelectType = null;
    private Spinner cboTronconID = null;
    
    private ArrayList<String> astrTroncon = new ArrayList<String>();    
    private ArrayList<String> astrType = new ArrayList<String>();
    
    private double dFeatureLatDeg = 0;
    private double dFeatureLonDeg = 0;
    
    private long[] alTronconID = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.describe);

		Bundle extras = getIntent().getExtras();
		if (extras == null)
			return;
		
		dFeatureLatDeg = extras.getDouble("dFeatureLat", 0);
		dFeatureLonDeg = extras.getDouble("dFeatureLon", 0);
		
		cboSelectType = (Spinner) findViewById(R.id.cboSelectType);
		ArrayAdapter<String> aa = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_dropdown_item, astrType);
		cboSelectType.setAdapter(aa);	
		aa.add("Borne d'incendie");
		aa.add("Puisart");

		cboTronconID = (Spinner) findViewById(R.id.cboTronconID);
				
        AsyncCallWS task = new AsyncCallWS();
        task.execute();				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		return true;
	}
	
    public void onClickOK(View v)
    {
    	Intent intent = new Intent(this, DistanceLocator.class);
    	
    	EditText txtUserID = (EditText) findViewById(R.id.txtUserID);
    	Spinner cboSelectType = (Spinner) findViewById(R.id.cboSelectType);
    	
    	intent.putExtra("strUserID", txtUserID.getText().toString());
    	intent.putExtra("strType", cboSelectType.getSelectedItem().toString());
    	intent.putExtra("strSection", String.format("%d",alTronconID[(int)cboTronconID.getSelectedItemId()]));

        // Finish activity
        setResult(Activity.RESULT_OK, intent);
        this.finish();
    	
    }
    
    public void onClickCancel(View v)
    {
    	Intent intent = new Intent(this, DistanceLocator.class);
        setResult(Activity.RESULT_CANCELED, intent);
    	this.finish();
    }    
    
    private void vFillTronconComboBox()
    {
    	ArrayAdapter<String> aa = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_dropdown_item, astrTroncon);
		this.cboTronconID.setAdapter(aa);
    }
	
        
	private class AsyncCallWS extends AsyncTask<String, Void, Void> 
	{
    	
    	final int ciNbMaxEntries = 10;

    	@Override
        protected Void doInBackground(String... params) {
        	vGetTronconFromWFS();
            return null;
        }
 
        @Override
        protected void onPostExecute(Void result) {
        	vFillTronconComboBox();
        }
 
        @Override
        protected void onPreExecute() {
        }
 
        @Override
        protected void onProgressUpdate(Void... values) {
        }
            
        private void vGetTronconFromWFS()
        {
            try {
            	
            	HttpClient httpClient = new DefaultHttpClient();

        		final String cstrURLTemplate = "http://192.168.1.109:8080/geoserver/popinfra/ows?" +
        	    		"service=WFS&version=1.0.0&request=GetFeature&typeName=popinfra:%s&" +
        	    		"maxFeatures=%d&&outputFormat=json&" +
        	    		"cql_filter=DWITHIN(%s,POINT(%f %f),%.3f,meters)";
            	
            	String strURL = String.format(Locale.ENGLISH, cstrURLTemplate, 
            			"GEOBASE_MTL_L", ciNbMaxEntries, "geometry", dFeatureLonDeg, dFeatureLatDeg, 0.001);
            	strURL = strURL.replace(" ", "%20");
            	
            	alTronconID = new long[ciNbMaxEntries];
            	
            	// Prepare a request object
                HttpGet httpget = new HttpGet(strURL); 
                HttpResponse response = httpClient.execute(httpget);
                HttpEntity entity = response.getEntity();

                if (entity != null) 
                {
                    // A Simple JSON Response parsing
                    InputStream is = entity.getContent();
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();

                    String line = null;
                    try 
                    {
                        while ((line = reader.readLine()) != null)
                            sb.append(line + "\n");                       
                    } catch (IOException e) {}
                        
                    String strResult = sb.toString();
                    int i = 0;
                    int k = 0;
                    
                    // "id_trc":1602102,"typ_voie":"avenue","lie_voie":null,"nom_voie":"Brock"
                    // "id_trc":1131001,"typ_voie":"avenue","lie_voie":"du","nom_voie":"Parc"
                    while ((i = strResult.indexOf("ID_TRC\":", i)) > -1)
                    {
                    	i += 8;
                    	int j = strResult.indexOf(",", i);
                    	String strID = strResult.substring(i, j);
                    	
                    	if ((i = strResult.indexOf("TYP_VOIE\":\"", i)) > -1)
                    	{
                        	i += 11;
                        	j = strResult.indexOf("\"", i);
                        	String strType = strResult.substring(i, j).trim();
                        	String strLie = "";
                        	String strNom = "";
                        	
	                    	if ((i = strResult.indexOf("LIE_VOIE\":\"", i)) > -1)
	                    	{
	                        	i += 11;
	                        	j = strResult.indexOf("\"", i);
	                        	strLie = strResult.substring(i, j).trim();
	                    	}
                    	
	                    	if ((i = strResult.indexOf("NOM_VOIE\":\"", i)) > -1)
	                    	{
	                        	i += 11;
	                        	j = strResult.indexOf("\"", i);
	                        	strNom = strResult.substring(i, j).trim();
	                        	
	                        	String strTroncon = strType;
	                        	if (strLie.length() > 0)
	                        		strTroncon += " " + strLie;
	                        	strTroncon += " " + strNom;
	                        	
	                        	if (!astrTroncon.contains(strTroncon))
	                        	{
	                        		astrTroncon.add(strTroncon);	                            	
	                                alTronconID[k++] = Long.parseLong(strID);                        		
	                        	}
	                    	}
                    	}
                    }
                    is.close();
                }

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
