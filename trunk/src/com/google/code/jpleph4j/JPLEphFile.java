package com.google.code.jpleph4j;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;

/**
 *
 * @author kaoe
 */
public class JPLEphFile {
    
    private final Object LOCK = new Object();
    private final RandomAccessFile raf;
    private JPLEphHeader header;
    private JPLEphConstant[] constants;

    public JPLEphFile(File file) throws FileNotFoundException {
        synchronized(LOCK) {
            raf = new RandomAccessFile(file, "r");
        }
    }
    
    public synchronized JPLEphHeader getHeader() throws IOException {
        if (header==null) {
            header = JPLEphHeader.parse(this);
        }
        return header;
    }
    
    public synchronized JPLEphConstant[] getConstants() throws IOException {
        if (constants==null) {
            constants = JPLEphConstant.getConstants(this);
        }
        return constants;
    }
    
    public void pleph(double et, JPLEphTarget ntarg, JPLEphTarget ncent, 
            double rrd[], boolean calcVelocity) throws IOException {
        
        JPLEphHeader eph = getHeader();
        
        double[][] pv = new double[13][6];
        
        int listVal = (calcVelocity ? 2 : 1);
        int[] list = new int[12];

        for(int i = 0; i < 6; ++i) rrd[i] = 0.0;
        
        if( ntarg == ncent || (ntarg!=null && ntarg.equals(ncent))) return;
        
        for(int i = 0; i < 12; i++) list[i] = 0;
        
        if(JPLEphTarget.NUTATIONS.equals(ntarg)) {
            if( eph.getIpt() [11][1] > 0) {
               list[10] = listVal;
               state(et, list, pv, rrd, false);
            }
            return;
        }

        if(JPLEphTarget.LIBRATIONS.equals(ntarg)) {
            if( eph.getIpt()[12][1] > 0) {
               list[11] = listVal;
               state(et, list, pv, rrd, false);
               for(int i = 0; i < 6; ++i) {
                   rrd[i] = pv[10][i];
               }
            }
            return;
        }
        
        if(ntarg==null || ncent==null || ntarg.ordinal() > 13 || ncent.ordinal() > 13 || ntarg.ordinal() < 1 || ncent.ordinal() < 1) return;
        
        for(int i = 0; i < 2; i++) {
            int k = ntarg.ordinal()-1;
            if( i == 1) k=ncent.ordinal()-1;
            if( k <= 9) list[k] = listVal;
            if( k == 9) list[2] = listVal;
            if( k == 2) list[9] = listVal;
            if( k == 12) list[2] = listVal;
        }

        state(et, list, pv, rrd, true);
        if(JPLEphTarget.SUN.equals(ntarg) || JPLEphTarget.SUN.equals(ncent)) {
            System.arraycopy(eph.getPvsun(), 0, pv[10], 0, 6);
        }

        if(JPLEphTarget.SSBARY.equals(ntarg) || JPLEphTarget.SSBARY.equals(ncent)) {
            for(int i = 0; i < 6; i++) {
                pv[11][i] = 0.0;
            }
        }

        if(JPLEphTarget.EMBARY.equals(ntarg) || JPLEphTarget.EMBARY.equals(ncent)) {
            System.arraycopy(pv[2], 0, pv[12], 0, 6);
        }

        if( (ntarg.ordinal()*ncent.ordinal()) == 30 && (ntarg.ordinal()+ncent.ordinal()) == 13) {
            for(int i = 0; i < 6; ++i) {
                pv[2][i]=0.0;
            }
        } else {
            if(list[2]>0) {
               for(int i = 0; i < list[2] * 3; ++i)
                  pv[2][i] -= pv[9][i]/(1.0+eph.getEmrat());
            }
            if(list[9]>0) {
               for(int i = 0; i < list[9] * 3; ++i) {
                   pv[9][i] += pv[2][i];
               }
            }
        }
        
        for(int i = 0; i < listVal * 3; ++i) {
            rrd[i] = pv[ntarg.ordinal()-1][i] - pv[ncent.ordinal()-1][i];
        }
        
    }
    
    private void state(double et, int[] list, double[][] pv, double[] nut, boolean bary) throws IOException {
        
        JPLEphHeader eph = getHeader();
      
        if (eph.getCache()==null) {
            eph.setCache(new double[eph.getNcoeff()]);
        }
        
        long nr;
        double[] buf = eph.getCache();
        
        double[] t = new double[2];
        double blockLoc = (et - eph.getEphemStart()) / eph.getEphemStep();
        double aufac = 1.0 / eph.getAu();
        boolean pvsunRecompute;

        nr = (long)blockLoc;
        t[0] = blockLoc - (double)nr;
        if(et == eph.getEphemEnd()){
            nr--;
            t[0] = 1.0 - 1e-16;
        }
        
        if( nr != eph.getCurrCacheLoc()) {
            eph.setCurrCacheLoc(nr);
            synchronized(LOCK) {
                this.seek((nr + 2) * eph.getRecSize());
                for (int i=0;i<buf.length;i++) {
                    buf[i] = this.readDouble();
                }
            }
        }
        t[1] = eph.getEphemStep();

        if (eph.getPvsunPos()!=et) {
            pvsunRecompute = true;
            eph.setPvsunPos(et);
        } else {
            pvsunRecompute = false;
        }
        
        for (int nIntervals=1;nIntervals<=8;nIntervals*=2) {
            for(int i = 0; i < 11; i++) {
                if(nIntervals == eph.getIpt()[i][2] && (list[i]>0 || (i == 10 && pvsunRecompute))) {
                    int flag = ((i == 10) ? 2 : list[i]);
                    double[] dest = ((i == 10) ? eph.getPvsun() : pv[i]);
                    interp(eph.getInfo(), buf, eph.getIpt()[i][0]-1, t, eph.getIpt()[i][1], 3, nIntervals, flag, dest);
                    for(int j = 0; j < flag * 3; j++) {
                       dest[j] *= aufac;
                    }
                }
            }
        
            if(!bary) {
               for(int i = 0; i < 9; i++) {
                  for(int j = 0; j < list[i] * 3; j++) {
                     pv[i][j] -= eph.getPvsun()[j];
                  }
               }
            }
            
            if(list[10] > 0 && eph.getIpt()[11][1] > 0) {
                interp(eph.getInfo(), buf, eph.getIpt()[11][0]-1, t, eph.getIpt()[11][1], 2, eph.getIpt()[11][2], list[10], nut);
            }

            if( list[11] > 0 && eph.getIpt()[12][1] > 0) {
                double[] pefau = new double[6];
                interp(eph.getInfo(), buf, eph.getIpt()[12][0]-1, t, eph.getIpt()[12][1], 3, eph.getIpt()[12][2], list[11], pefau);
                for(int j = 0; j < 6; ++j) {
                   pv[10][j] = pefau[j];
                }
            }

        }
        
    }
    
    private static double modf(double valor) {
        BigDecimal intPart = BigDecimal.valueOf((long)valor);
        return BigDecimal.valueOf(valor).subtract(intPart).doubleValue();
    }
    
    private void interp(JPLEphInterpolationInfo info, double[] coef, int coefStart, double t[], int ncf, int ncm,
            int na, int velocityFlag, double posvel[]) throws IOException {
        
        double dna = na;
        double temp = dna * t[0];
        int l = (int)temp;
        double vfac;
        double tc = 2.0 * modf( temp) - 1.0;

        if( tc != info.getPc()[1]) {
           info.setNp(2);
           info.setNv(3);
           info.getPc()[1] = tc;
           info.setTwot(tc+tc);
        }

        if( info.getNp() < ncf) {
            int pcPtr = info.getNp();
            for(int i=ncf - info.getNp(); i>0; i--, pcPtr++) {
                info.getPc()[pcPtr] = info.getTwot() * info.getPc()[pcPtr-1] -info.getPc()[pcPtr-2];
            }
            info.setNp(ncf);
        }
        
        int posvelPtr = 0;
        for(int i = 0; i < ncm; ++i) {
            int coeffPtr = coefStart + ncf * (i + l * ncm + 1);
            int pcPtr = ncf;
            posvel[posvelPtr] = 0.0;
            for(int j = ncf; j>0; j--) {
                posvel[posvelPtr] += (info.getPc()[--pcPtr]) * (coef[--coeffPtr]);
            }
            posvelPtr++;
        }

        if( velocityFlag <= 1) return;

        info.getVc()[2] = info.getTwot() + info.getTwot();
        if( info.getNv() < ncf) {
           int vcPtr = info.getNv();
           int pcPtr = info.getNv() - 1;
           for(int i = ncf - info.getNv(); i>0; i--, vcPtr++, pcPtr++) {
               info.getVc()[vcPtr] = info.getTwot() * info.getVc()[vcPtr-1] + info.getPc()[pcPtr] + info.getPc()[pcPtr] - info.getVc()[vcPtr-2];
           }
           info.setNv(ncf);
        }

        vfac = (dna + dna) / t[1];
        for(int i = 0; i < ncm; ++i) {
           double tval = 0.;
           int coeffPtr = coefStart + ncf * (i + l * ncm + 1);
           int vcPtr = ncf;
           for(int j = ncf; j>0; j--) {
               tval += (info.getVc()[--vcPtr]) * (coef[--coeffPtr]);
           }
           posvel[posvelPtr++] = tval * vfac;
        }

    }
    
    public void clean() {
        if (header!=null) {
            header.cleanLocalCache();
        }
    }
    
    public void close() throws IOException {
        if (header!=null) {
            header.cleanLocalCache();
            header = null;
        }
        if (constants!=null) {
            constants = null;
        }
        synchronized(LOCK) {
            raf.close();
        }
    }

    public Object getLock() {
        return LOCK;
    }
    
    public void seek(long pos) throws IOException {
        raf.seek(pos);
    }

    public int skipBytes(int n) throws IOException {
        return raf.skipBytes(n);
    }
    
    public final void readFully(byte b[]) throws IOException {
        raf.readFully(b);
    }
    
    public final void readFully(byte b[], int off, int len) throws IOException {
        raf.readFully(b, off, len);
    }
    
    public int readInt() throws IOException {
        int ch4 = raf.read();
        int ch3 = raf.read();
        int ch2 = raf.read();
        int ch1 = raf.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    }
    
    private long readLong() throws IOException {
        byte readBuffer[] = new byte[8];
        raf.readFully(readBuffer, 0, 8);
        return (((long)readBuffer[7] << 56) +
                ((long)(readBuffer[6] & 255) << 48) +
                ((long)(readBuffer[5] & 255) << 40) +
                ((long)(readBuffer[4] & 255) << 32) +
                ((long)(readBuffer[3] & 255) << 24) +
                ((readBuffer[2] & 255) << 16) +
                ((readBuffer[1] & 255) <<  8) +
                ((readBuffer[0] & 255)));
    }
    
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

}
