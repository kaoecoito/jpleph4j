package com.google.code.jpleph4j;

/**
 *
 * @author kaoe
 */
public class JPLEphInterpolationInfo {
    
    private final double[] pc,vc;
    private double twot;
    private int np, nv;

    public JPLEphInterpolationInfo() {
        pc = new double[18];
        vc = new double[18];
    }

    public double[] getPc() {
        return pc;
    }

    public double[] getVc() {
        return vc;
    }

    public double getTwot() {
        return twot;
    }

    public void setTwot(double twot) {
        this.twot = twot;
    }

    public int getNp() {
        return np;
    }

    public void setNp(int np) {
        this.np = np;
    }

    public int getNv() {
        return nv;
    }

    public void setNv(int nv) {
        this.nv = nv;
    }
    
}
