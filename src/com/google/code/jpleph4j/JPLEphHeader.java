package com.google.code.jpleph4j;

import java.io.IOException;

/**
 *
 * @author kaoe
 */
public class JPLEphHeader {
    
    public static final double J2000 = 2451545.0;
    
    private String title;
    private double ephemStart, ephemEnd, ephemStep;
    private int ncon;
    private double au;
    private double emrat;
    int[][] ipt;
    private int ephemerisVersion;
    
    private int kernelSize, recSize, ncoeff;

    private final ThreadLocal<JPLEphCache> localCache = new ThreadLocal<JPLEphCache>();

    private JPLEphHeader() {
        ipt = new int[13][3];
    }
    
    static JPLEphHeader parse(JPLEphFile file) throws IOException {
        final Object LOCK = file.getLock();
        JPLEphHeader header = new JPLEphHeader();
        int lastIpt;
        
        synchronized(LOCK) {
            byte[] titleBuffer = new byte[84];
            file.readFully(titleBuffer);
            header.title = (new String(titleBuffer)).trim();

            file.seek(2652l);

            header.ephemStart = file.readDouble();
            header.ephemEnd = file.readDouble();
            header.ephemStep = file.readDouble();
            header.ncon = file.readInt();
            header.au = file.readDouble();
            header.emrat = file.readDouble();

            if( header.getEmrat() > 81.3008 || header.getEmrat() < 81.30055) {
                throw new IOException("Corrupt JPL DEXXX file");
            }

            for(int i = 0; i < 13; i++) {
                for(int j = 0; j < 3; j++) {
                   header.ipt[i][j] = file.readInt();
                }
            }
            
            lastIpt = file.readInt();
        }

        header.ipt[12][0] = header.ipt[12][1];
        header.ipt[12][1] = header.ipt[12][2];
        header.ipt[12][2] = lastIpt;

        header.ephemerisVersion = Integer.parseInt(header.title.substring(26, 29));
        
        header.kernelSize = 4;
        for(int i = 0; i < 13; i++) {
           header.kernelSize += header.ipt[i][1] * header.ipt[i][2] * ((i == 11) ? 4 : 6);
        }
        header.recSize = (int)(header.kernelSize * 4l);
        header.ncoeff = (int)(header.kernelSize / 2l);

        header.getInfo().setNp(2);
        header.getInfo().setNv(3);
        header.getInfo().getPc()[0] = 1.0;
        header.getInfo().getPc()[1] = 0.0;
        header.getInfo().getVc()[1] = 1.0;

        return header;
    }
    
    private JPLEphCache getLocalCache() {
        if (localCache.get()==null) {
            localCache.set(new JPLEphCache());
        }
        return localCache.get();
    }
    
    public void cleanLocalCache() {
        localCache.remove();
    }
    
    public String getTitle() {
        return title;
    }

    public double getEphemStart() {
        return ephemStart;
    }

    public double getEphemEnd() {
        return ephemEnd;
    }

    public double getEphemStep() {
        return ephemStep;
    }

    public int getNcon() {
        return ncon;
    }

    public double getAu() {
        return au;
    }

    public double getEmrat() {
        return emrat;
    }

    public int[][] getIpt() {
        return ipt;
    }

    public int getEphemerisVersion() {
        return ephemerisVersion;
    }

    public int getKernelSize() {
        return kernelSize;
    }

    public int getRecSize() {
        return recSize;
    }

    public int getNcoeff() {
        return ncoeff;
    }

    public double[] getPvsun() {
        return getLocalCache().getPvsun();
    }

    public double getPvsunPos() {
        return getLocalCache().getPvsunPos();
    }

    public void setPvsunPos(double pvsunPos) {
        getLocalCache().setPvsunPos(pvsunPos);
    }

    public long getCurrCacheLoc() {
        return getLocalCache().getCurrCacheLoc();
    }

    public void setCurrCacheLoc(long currCacheLoc) {
        getLocalCache().setCurrCacheLoc(currCacheLoc);
    }

    public double[] getCache() {
        return getLocalCache().getCache();
    }

    public void setCache(double[] cache) {
        getLocalCache().setCache(cache);
    }

    public JPLEphInterpolationInfo getInfo() {
        return getLocalCache().getInfo();
    }

}
