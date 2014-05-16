package com.google.code.jpleph4j;

/**
 *
 * @author kaoe
 */
public class JPLEphCache {
    
    private final double[] pvsun;
    private double pvsunPos;
    private long currCacheLoc;
    private double[] cache;
    
    private final JPLEphInterpolationInfo info;

    public JPLEphCache() {
        pvsun = new double[6];
        currCacheLoc = -1l;
        info = new JPLEphInterpolationInfo();
    }

    public double[] getPvsun() {
        return pvsun;
    }

    public double getPvsunPos() {
        return pvsunPos;
    }

    public void setPvsunPos(double pvsunPos) {
        this.pvsunPos = pvsunPos;
    }

    public long getCurrCacheLoc() {
        return currCacheLoc;
    }

    public void setCurrCacheLoc(long currCacheLoc) {
        this.currCacheLoc = currCacheLoc;
    }

    public double[] getCache() {
        return cache;
    }

    public void setCache(double[] cache) {
        this.cache = cache;
    }

    public JPLEphInterpolationInfo getInfo() {
        return info;
    }

}
