package com.acsg.geohack;

public class VdMat 
{
	public int NbRows = 0;
	public int NbCols = 0;
	private double[] adContent = null; 
	
	public VdMat(int iRows, int iCols)
	{
		this.NbRows = iRows;
		this.NbCols = iCols;
		this.adContent = new double[iRows * iCols];
	}
	
	public double dGet(int i, int j) throws Exception
	{
		int iIndex = i * this.NbCols + j;
		if (iIndex > this.adContent.length)
			throw new Exception("Out of bounds");
		
		return this.adContent[iIndex];
	}

	public void vSet(int i, int j, double dValue) throws Exception
	{
		int iIndex = i * this.NbCols + j;
		if (iIndex > this.adContent.length)
			throw new Exception("Out of bounds");

		this.adContent[iIndex] = dValue;
	}
	
	
    public static VdMat Multiply(VdMat vdMat1, VdMat vdMat2) throws Exception
    {
        if (vdMat1.NbCols != vdMat2.NbRows)
            throw new Exception("Dimension mismatch between matrices");
        
        VdMat vdReturnMat = new VdMat(vdMat1.NbRows, vdMat2.NbCols);
        
        for (int i = 0; i < vdMat1.NbRows; i++ )
        {
            for (int j = 0; j < vdMat2.NbCols; j++)
            {
                double dVal = 0;
                for (int k = 0; k < vdMat1.NbCols; k++)
                    dVal += (vdMat1.dGet(i, k) * vdMat2.dGet(k, j));
                vdReturnMat.vSet(i, j, dVal);
            }
        }
        
        return vdReturnMat;
    }
    
    public VdMat Transpose() throws Exception
    {
        VdMat vdReturnMat = new VdMat(this.NbCols, this.NbRows);
        for (int i = 0; i < this.NbRows; i++)
        {
            for (int j = 0; j < this.NbCols; j++)
                vdReturnMat.vSet(j, i, this.dGet(i, j));
        }
        return vdReturnMat;
    }
    
    
    private boolean LUP_crout( int[] aiIndex) throws Exception
    {
        // LU decomposition using Crout
        int i, j, k, imax=0;
        double sum, big, dum, dTemp;
        
        // "Non square matrix! Cannot perform LU decomposition"
        if (NbRows != NbCols)
            return false;

        int n = NbRows;

        // "Index vector not properly dimensioned in LU decomposition"
        if (aiIndex.length != NbRows)
            return false;

        VdMat vv = new VdMat(n, 1);

        // Find biggest element on each row
        for (i = 0; i < n; i++)
        {
            big = 0.0;
            for (j = 0; j < n; j++)
            {
                if ((dTemp = Math.abs(this.dGet(i, j))) > big)
                    big = dTemp;
            }
            
            // throw new Exception("Singular matrix in LUP matrix decomposition");
            if (big == 0.0)
                return false;
            
            // Save the scaling
            vv.vSet(i, 0, 1.0 / big);
        }

        for (j = 0; j < n; j++)
        {
            for (i = 0; i < j; i++)
            {
                sum = this.dGet(i, j);
                for (k = 0; k < i; k++)
                    sum = sum - this.dGet(i,k) * this.dGet(k,j);
                this.vSet(i,j,sum);
            }
            
            big = 0.0;
            for (i = j; i < n; i++)
            {
                sum = this.dGet(i,j);
                for (k = 0; k < j; k++)
                    sum = sum - this.dGet(i,k) * this.dGet(k,j);
                this.vSet(i, j, sum);

                if ((dum = vv.dGet(i,0) * Math.abs(sum)) >= big)
                {
                    big = dum;
                    imax = i;
                }
            }

            if (j != imax)
            {
                for (k = 0; k < n; k++)
                {
                    dum = this.dGet(imax, k);
                    this.vSet(imax, k, this.dGet(j, k));
                    this.vSet(j, k, dum);
                }
                vv.vSet(imax, 0, vv.dGet(j,0));
            }
            aiIndex[j] = imax;
            
            // throw new Exception("Singular matrix in LUP matrix decomposition");
            if (this.dGet(j,j) == 0.0)
                return false;
            
            if (j < (n-1))
            {
                dum = 1.0 / this.dGet(j,j);
                for (i = j+1; i < n; i++)
                    this.vSet(i, j, this.dGet(i, j) * dum);
            }
        }
        return true;
    
    }


    private void Solve_LUP(int[] aiIndex, VdMat vdX) throws Exception
    {
        int i, ii = 0, ip, j;
        double sum;
        boolean boFirst = true;
        if (vdX.NbRows != NbRows)
        {
            throw new Exception("Result vector (X) not properly dimensioned in LU solve");
        }
        
        int n = NbRows;
        for (i = 0; i < n; i++)
        {
            ip = aiIndex[i];
            sum = vdX.dGet(ip,0);
            vdX.vSet(ip, 0, vdX.dGet(i,0));
            
            if (!boFirst)
            {
                for (j=ii; j<i; j++)
                    sum -= this.dGet(i, j) * vdX.dGet(j,0);
            }
            else if (sum != 0.0)
            {
                boFirst = false;
                ii = i;
            }
            vdX.vSet(i, 0, sum);
        }
        
        for (i = n; i > 0; i--)
        {
            sum = vdX.dGet(i-1, 0);
            for (j = i; j < n; j++)
                sum -= this.dGet(i-1,j) * vdX.dGet(j,0);
            vdX.vSet(i-1, 0, sum / this.dGet(i-1,i-1));
        }
    }

    
    public boolean boInvert() throws Exception
    {
        VdMat Inverse = new VdMat(NbRows, NbCols);
        int[] Indx =  new int[NbRows];

        if (!LUP_crout(Indx))
            return false;

        VdMat X =  new VdMat(NbRows,1);

        for (int j = 0; j < NbRows; j++)
        {
            for (int i = 0; i < NbRows; i++)
                X.vSet(i, 0, 0.0);
            X.vSet(j, 0, 1.0);

            Solve_LUP(Indx, X);

            for (int i = 0; i < NbRows; i++)
                Inverse.vSet(i,j, X.dGet(i,0));
        }
        for (int i = 0; i < NbRows; i++)
        {
            for (int j = 0; j < NbRows; j++)
                this.vSet(i, j, Inverse.dGet(i, j));
        }
        
        return true;
    }    	
}
