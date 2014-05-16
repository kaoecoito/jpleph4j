package com.google.code.jpleph4j;

import java.io.IOException;

/**
 *
 * @author kaoe
 */
public class JPLEphConstant {
    
    private String name;
    private double constant;

    private JPLEphConstant() {
    }
    
    static JPLEphConstant[] getConstants(JPLEphFile file) throws IOException {
        JPLEphConstant[] consts = new JPLEphConstant[file.getHeader().getNcon()];
        for (int i=0;i<consts.length;i++) {
            int idx = i / 2 + (i & 1) * (consts.length + 1) / 2;
            consts[i] = getEphConstant(file, idx);
        }
        return consts;
    }
    
    private static JPLEphConstant getEphConstant(JPLEphFile file, int idx) throws IOException {
        
        JPLEphHeader header = file.getHeader();
        
        JPLEphConstant constant = new JPLEphConstant();
        
        final Object LOCK = file.getLock();
        
        synchronized(LOCK) {
            if( idx >= 0 && idx < header.getNcon()) {
                byte[] buffer = new byte[6];

                file.seek(84L * 3l + idx * 6);
                file.readFully(buffer);
                constant.name = (new String(buffer)).trim();

                file.seek( header.getRecSize() + idx * 8);
                constant.constant = file.readDouble();
            }
        }
        
        return constant;
    }

    public String getName() {
        return name;
    }

    public double getConstant() {
        return constant;
    }
    
}
