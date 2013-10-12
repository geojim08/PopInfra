package com.acsg.geohack;

import java.util.ArrayList;

import com.acsg.Conversion.GeodeticStruct;

public class DistanceLSQ 
{
	private final static double stcdOffsetX = 1000.0;
	private final static double stcdOffsetY = 1000.0;
	private final static int stciMaxIter = 50;
	private final static double stcdConvergenceLimit = 0.01;
	
    public class CoordinateStruct
    {
    	public double dLat;
    	public double dLon;
    	public double dDistance;
    	public float fAccuracy;
    }
	
	private ArrayList<CoordinateStruct> ListObs = new ArrayList<CoordinateStruct>();
	private double dOriginLat;
	private double dOriginLon;
	private double dLatDegToM;
	private double dLonDegToM;
	
	private boolean boConverge;
	
	private VdMat vdX0 = null;
	private VdMat vdW = null;
	private VdMat mdNInverse = null;
	private double dVtPV = 0;

	public DistanceLSQ()
	{
		this.vReset();
	}
	
	public int iGetNbObs()
	{
		return this.ListObs.size();
	}
	
	public void vReset()
	{
		this.ListObs.clear();
		this.dOriginLat = this.dOriginLon = this.dLatDegToM = this.dLonDegToM = 0; 	
		this.boConverge = false;
		this.vdX0 = new VdMat(2,1);
		this.vdW = null;
		this.mdNInverse = null;
		this.dVtPV = 0;
	}
	
	
	public void vAddObs(double dLatDeg, double dLonDeg, float fAccuracyM, double dDistM)
	{
		if (ListObs.size() == 0)
		{
			// Set origin.
			this.dOriginLat = dLatDeg;
			this.dOriginLon = dLonDeg;
			
			// Compute conversion factors.
			double[] adCoord = { dLatDeg, dLonDeg, 0 };
			this.dLatDegToM = GeodeticStruct.stdGetWGS84MeridianScale(adCoord[0]);
			this.dLonDegToM = GeodeticStruct.stdGetWGS84LatitudeCircleScale(adCoord);
		}
			
		// Set a local system.
		CoordinateStruct s = new CoordinateStruct();
		s.dLat = stcdOffsetY + (dLatDeg - this.dOriginLat) * this.dLatDegToM;
		s.dLon = stcdOffsetX + (dLonDeg - this.dOriginLon) * this.dLonDegToM;
		s.dDistance = dDistM;
		s.fAccuracy = fAccuracyM;
		
		ListObs.add(s);
	}

	public boolean boCompute() throws Exception
	{
		if (this.ListObs.size() < 2)
			return false;

		// Get an initial value for X0.
		double dN = 0, dE = 0;
		int iNbObs = this.ListObs.size();
		for (int i = 0; i < iNbObs; i++)
		{
			dN += this.ListObs.get(i).dLat;
			dE += this.ListObs.get(i).dLon;
		}
		dN /= iNbObs;
		dE /= iNbObs;
		
		int iIter = 0;
		this.boConverge = false;
		VdMat mdA = null;
		VdMat mdP = null;
		this.vdW = null;
		VdMat vdXHat = new VdMat(2, 1);

		while(!this.boConverge)
		{
			dN += vdXHat.dGet(0,0);
			dE += vdXHat.dGet(1,0);
			this.vdX0.vSet(0, 0, dN);
			this.vdX0.vSet(1, 0, dE);

			// Model id Dx = sqrt((Nx - N0)^2 + (Ex - E0)^2).
			mdA = new VdMat(iNbObs, 2);
			this.vdW = new VdMat(iNbObs, 1);
			mdP = new VdMat(iNbObs, iNbObs);
			
			for (int i = 0; i < iNbObs; i++)
			{
				CoordinateStruct s = this.ListObs.get(i);
				double dDeltaN = s.dLat - this.vdX0.dGet(0, 0);
				double dDeltaE = s.dLon - this.vdX0.dGet(1, 0);
				double dRho = Math.sqrt(dDeltaN * dDeltaN + dDeltaE * dDeltaE);
				
				mdA.vSet(i, 0, -dDeltaN / dRho);
				mdA.vSet(i, 1, -dDeltaE / dRho);
				mdP.vSet(i, i, 1.0 / (s.fAccuracy * s.fAccuracy));
				this.vdW.vSet(i, 0, s.dDistance - dRho);		    
			}
			
			VdMat mdAtP = VdMat.Multiply(mdA.Transpose(), mdP);
			VdMat mdN = VdMat.Multiply(mdAtP, mdA);
			VdMat vdU = VdMat.Multiply(mdAtP, vdW);
			
			this.mdNInverse = mdN;
			if (!this.mdNInverse.boInvert())
				return false;
			
			vdXHat = VdMat.Multiply(this.mdNInverse, vdU);
			
			this.boConverge = 
					(Math.abs(vdXHat.dGet(0,0)) < stcdConvergenceLimit && 
					Math.abs(vdXHat.dGet(1,0)) < stcdConvergenceLimit);
			
			if (++iIter > stciMaxIter)
				break;
		}

		// GDOP
/*		
		VdMat mdAtAInverse = VdMat.Multiply(mdA.Transpose(), mdA);
		mdAtAInverse.boInvert();
		double dGDOP = Math.sqrt(mdAtAInverse.dGet(0, 0) + mdAtAInverse.dGet(1, 1));
		
		// In theory, center-to-center distance should be smaller to the sum of 
		// the measured distances :
		// It appears that the LSQ has more chance of success (always?) if at least 
		// 2 pairs meet this rule.
		int iNbOK = 0;
		for (int i = 0; i < iNbObs; i++)
		{
			int iNext = (i == iNbObs-1) ? 0 : i+1;
			CoordinateStruct s1 = this.ListObs.get(i);
			CoordinateStruct s2 = this.ListObs.get(iNext);
			
			dN = s2.dLat - s1.dLat;
			dE = s2.dLon - s1.dLon;
			double dCtoC = Math.sqrt(dN*dN + dE*dE);
			if (dCtoC - (s2.dDistance + s1.dDistance) <= 0)
				iNbOK++;
		}		
*/		
		
		if (this.boConverge)
		{
			VdMat vdWt = this.vdW.Transpose();
			VdMat vdVtP = VdMat.Multiply(vdWt, mdP);
			VdMat vdVtPV = VdMat.Multiply(vdVtP, this.vdW);
			this.dVtPV = vdVtPV.dGet(0,0);
		}
		
		
		return this.boConverge;
	}
	
	public CoordinateStruct GetResult()
	{
		if (!this.boConverge)
			return null;

		CoordinateStruct s = null;
		
		try
		{
			// Position result.
			double dLat = this.dOriginLat + (this.vdX0.dGet(0, 0) - stcdOffsetY) / this.dLatDegToM;
			double dLon = this.dOriginLon + (this.vdX0.dGet(1, 0) - stcdOffsetX) / this.dLonDegToM;
			
			// Variance's aposteriori factor.
			double dDegFreedom = (this.iGetNbObs() > 2) ? this.iGetNbObs() - 2 : 1;						
			double dVarFactor = (this.dVtPV / dDegFreedom);						
			
			// Accuracy estimation.
			double dAccuracy = (dVarFactor * this.mdNInverse.dGet(0,0) + 
					dVarFactor * this.mdNInverse.dGet(1,1));
			
			s = new CoordinateStruct();
			s.dLat = dLat;
			s.dLon = dLon;
			s.fAccuracy = (float)Math.sqrt(dAccuracy);
		} catch(Exception ex){}
		
		return s;
	}
	
	public VdMat vdGetResiduals() throws Exception
	{
		// This is not the real residuals since the real ones should be -vdW...
		if (!this.boConverge)
			return null;
		
		return this.vdW;
	}	
}
