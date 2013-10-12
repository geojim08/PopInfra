package com.acsg.Conversion;

public class GeodeticStruct 
{
	public static final double stcdDeg2Rad = Math.PI / 180.0;
	private static final double dWGS84EllipsA = 6378137.0;
	private static final double dWGS84EllipsB = 6356752.3142;
		
	public static final char stccNorth = 'N';
	public static final char stccSouth = 'S';
	public static final char stccEast = 'E';
	public static final char stccWest = 'W';

	// Decimal degrees.
	public double dLatDeg;
	public double dLonDeg;
	public double dEllHeightM;
	
	// Radians.
	public double dLatRad;
	public double dLonRad;
	
	// DMS. 
	public char cLatHemis;
	public int iLatDeg;
	public int iLatMin;
	public double dLatSec;
	public char cLonHemis;
	public int iLonDeg;
	public int iLonMin;
	public double dLonSec;
			
	public GeodeticStruct(boolean cboLatPositive, int ciLatDeg, 
			int ciLatMin, double cdLatSec, boolean cboLonPositive, 
			int ciLonDeg, int ciLonMin, double cdLonSec, 
			double cdEllHeightM)
	{
		cLatHemis = (cboLatPositive) ? stccNorth : stccSouth;
		iLatDeg = ciLatDeg;
		iLatMin = ciLatMin;
		dLatSec = cdLatSec;
		
		cLonHemis = (cboLonPositive) ? stccEast : stccWest;
		iLonDeg = ciLonDeg;
		iLonMin = ciLonMin;
		dLonSec = cdLonSec;
		
		dEllHeightM = cdEllHeightM;
		
		vSetCoordFromDMS();
	}
	
	
	public GeodeticStruct(double cdLat, double cdLon, 
			double cdEllHeightM, boolean cboUnitIsDeg)
	{
		dEllHeightM = cdEllHeightM;
		
		if (cboUnitIsDeg)
		{
			dLatDeg = cdLat;
			dLonDeg = cdLon;
			dLatRad = dLatDeg * stcdDeg2Rad;
			dLonRad = dLonDeg * stcdDeg2Rad;
		}
		else
		{
			dLatRad = cdLat;
			dLonRad = cdLon;
			dLatDeg = dLatRad / stcdDeg2Rad;
			dLonDeg = dLonRad / stcdDeg2Rad;
		}
		
		vSetCoordFromDeg();
	}
	
	
	public boolean boIsValid()
	{
		return (dLatDeg >= -90 && dLatDeg <= 90 && 
				dLonDeg >= -180 && dLonDeg <= 180); 
	}

	public static double[] stadConvertECEFToWGS84(double[] adECEF) 
	{
		//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//
		// adECEF must be in meters. 
		// Return values are lat(deg)/lon(deg)/height(m)
		// 
		//  from: Hofmann-Wellenhof, Lichtenegger & Collins
		//        "GPS Theory and Practice" (second edition) 1993
		//        Edition Springer-Verlag Wien New York
		//        p. 232
		//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//

		// Compute square of ellipsoid eccentricity
		double[] adGeoDeg = new double[3];
		double dEcc2 = stdComputeEcc2(dWGS84EllipsA, dWGS84EllipsB);
		double dB2 = dWGS84EllipsB * dWGS84EllipsB;
		double ep2 = (dWGS84EllipsA * dWGS84EllipsA - dB2) / dB2;

		double p = Math.sqrt(adECEF[0] * adECEF[0] + adECEF[1] * adECEF[1]);
		double teta = Math.atan2( adECEF[2] * dWGS84EllipsA, p * dWGS84EllipsB );
		double sinTeta = Math.sin(teta);
		double cosTeta = Math.cos(teta);

		// Evaluate latitude
		if (adECEF[2] != 0)
		{
			double dVal1 = (adECEF[2] +
					ep2 * dWGS84EllipsB * sinTeta * sinTeta * sinTeta);
			double dVal2 = (p - dEcc2 * dWGS84EllipsA * cosTeta * cosTeta * cosTeta);
			adGeoDeg[0] = Math.atan2(dVal1, dVal2);
		}
		else
		{
			// ...no Z means no latitude!
			adGeoDeg[0] = 0.0;
		}
		
		// Longitude
		adGeoDeg[1] = Math.atan2(adECEF[1], adECEF[0]);

		// Altitude
		if ( Math.abs(p) < 1e-6 )
		{
			// no p means we're at one of the poles
			// ...or at the center of the earth!
			if ( adECEF[2] > 0 )
				adGeoDeg[2] = adECEF[2] - dWGS84EllipsB;
			else if ( adECEF[2] < 0 )
				adGeoDeg[2] = -adECEF[2] - dWGS84EllipsB;
			else
				adGeoDeg[2] = -dWGS84EllipsA;
		}
		else
		{
			double sinLat = Math.sin(adGeoDeg[0]);
			double N = dWGS84EllipsA / Math.sqrt(1.0 - dEcc2 * sinLat * sinLat);
			adGeoDeg[2] = p / Math.cos(adGeoDeg[0]) - N;
		}
		
		// Convert lat/lon to degrees.
		adGeoDeg[0] /= stcdDeg2Rad;
		adGeoDeg[1] /= stcdDeg2Rad;
		
		return adGeoDeg;
	}
	
	
	public static double[] stadConvertWGS84ToECEF(double[] adGeoDeg)
	{
		//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//
		// adGeoDeg must be in deg(lat/lon) and meters(height). 
		// Return values are X/Y/Z (m)
		//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//**//
		
		double[] adECEF = new double[3];
		double E2 = stdComputeEcc2(dWGS84EllipsA, dWGS84EllipsB);

		double[] adRad = { adGeoDeg[0] * stcdDeg2Rad, 
				adGeoDeg[1] * stcdDeg2Rad, adGeoDeg[2] }; 

		double Cphi = Math.cos(adRad[0]);
		double Sphi = Math.sin(adRad[0]);
		double w = Math.sqrt(1.0 - (E2 * Sphi * Sphi));
		double N = dWGS84EllipsA / w;

		double dFact = (N + adRad[2]) * Cphi;
		adECEF[0] = dFact * Math.cos(adRad[1]);
		adECEF[1] = dFact * Math.sin(adRad[1]);
		adECEF[2] = ((dWGS84EllipsB * dWGS84EllipsB) / 
				(dWGS84EllipsA * w) + adRad[2]) * Sphi;
		return adECEF;
	}

	
	public static double stdGetWGS84LatitudeCircleScale(double[] adGeoCoord) 
	{
		// Find the X,Y radius to insert in Arc(m) = Radius * Teta(deg)*Deg2Rad.
	    double[] sXYZ = stadConvertWGS84ToECEF(adGeoCoord);
	    double dRadius = Math.sqrt(sXYZ[0] * sXYZ[0] + sXYZ[1] * sXYZ[1]);
	    return dRadius * stcdDeg2Rad;
	}
	
	public static double stdGetWGS84MeridianScale(double dDeg) 
	{
		// This method is used to convert latitude component in deg to meter
		// It corresponds to parameter M
		double dLatRad = dDeg * stcdDeg2Rad;
		
	    // Compute square of ellipsoid eccentricity
	    double dEcc2 = stdComputeEcc2(dWGS84EllipsA, dWGS84EllipsB);

	    double dSin = Math.sin(dLatRad);
	    double dTerm = 1.0 - dEcc2 * dSin * dSin;
	    double dTerm3 = dTerm;
	    dTerm3 *= dTerm;
	    dTerm3 *= dTerm;

	    // M = a*(1-e2) / (1 - e2*sinLat^2)^(3/2)
	    double dM = dWGS84EllipsA * (1.0 - dEcc2) / Math.sqrt(dTerm3);
	    return dM * stcdDeg2Rad;
	}
	
	
	public static double stdComputeHeading(double DN, double DE)
	{
		// Get the azimuth in degrees from geodetic components.
		double dHeading;
	
		if(DN != 0 || DE != 0)
			dHeading = Math.atan2(DE, DN);
		else
			dHeading = 0.0;
	
		if(dHeading < 0)
			dHeading = 2.0 * Math.PI + dHeading;
		
		dHeading /= stcdDeg2Rad;
		return dHeading;
	}
	
		
	private static double stdComputeEcc2(double dEllipsA,
			double dEllipsB)
	{
		double dA2 = dEllipsA * dEllipsA;
		double dB2 = dEllipsB * dEllipsB;
		return (dA2 - dB2) / dA2;
	}
		
	private void vSetCoordFromDeg()
	{
		double d = Math.abs(dLatDeg);
		iLatDeg = (int)d;
		d = (d - iLatDeg) * 60.0;
		iLatMin = (int)d;
		dLatSec = (d - iLatMin) * 60.0;	
		// Round it to the 5th digit (+/-0.3mm)
		dLatSec = (int)(dLatSec * 100000 + 0.5) / 100000.0;
		
		if (Math.abs(dLatSec - 60) < 1e-5)
		{
			dLatSec = 0.0;
			iLatMin++;
		}
		
		if (iLatMin == 60)
		{
			iLatMin = 0;
			iLatDeg++;
		}
		
		cLatHemis = (dLatDeg < 0) ? stccSouth : stccNorth;

		d = Math.abs(dLonDeg);
		iLonDeg = (int)d;
		d = (d - iLonDeg) * 60.0;
		iLonMin = (int)d;
		dLonSec = (d - iLonMin) * 60.0;		
		// Round it to the 5th digit (+/-0.2mm)
		dLonSec = (int)(dLonSec * 100000 + 0.5) / 100000.0;
		
		if (Math.abs(dLonSec - 60) < 1e-5)
		{
			dLonSec = 0.0;
			iLonMin++;
		}
		
		if (iLonMin == 60)
		{
			iLonMin = 0;
			iLonDeg++;
		}
		
		cLonHemis = (dLonDeg < 0) ? stccWest : stccEast;
		
	}
	
	
	private void vSetCoordFromDMS()
	{
		dLatDeg = iLatDeg + iLatMin / 60.0 + dLatSec / 3600.0;
		if (cLatHemis == stccSouth)
			dLatDeg *= -1;
		
		dLonDeg = iLonDeg + iLonMin / 60.0 + dLonSec / 3600.0;
		if (cLonHemis == stccWest)
			dLonDeg *= -1;
				
		dLatRad = dLatDeg * stcdDeg2Rad;
		dLonRad = dLonDeg * stcdDeg2Rad;
	}	
}
