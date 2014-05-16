package com.google.code.jpleph4j;

import java.io.File;

/**
 *
 * @author kaoe
 */
public class JPLEph {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        new JPLEph().teste();
    }
    
    
    private void teste() throws Exception {
        
        File file = new File("/home/kaoe/Downloads/eph_de431/lnxm13000p17000.431");
        JPLEphFile fis = new JPLEphFile(file);

        JPLEphHeader header = fis.getHeader();
        
        System.out.println("Title: "+header.getTitle());
        System.out.println("Start: "+header.getEphemStart());
        System.out.println("End: "+header.getEphemEnd());
        System.out.println("Step: "+header.getEphemStep());
        System.out.println("NCon: "+header.getNcon());
        System.out.println("Au: "+header.getAu());
        System.out.println("Emrat: "+header.getEmrat());
        System.out.println("Version: "+header.getEphemerisVersion());
        
        System.out.println("Kernel Size: "+header.getKernelSize());
        System.out.println("Record Size: "+header.getRecSize());
        System.out.println("N Coeff: "+header.getNcoeff());

        /*
        JPLEphConstant[] constants = fis.getConstants();
        for(JPLEphConstant constant:constants) {
            System.out.println(constant.getName()+" "+constant.getConstant());
        }
        */
        
        final double AU_IN_KM = header.getAu();
        
        double jd = 1500;
        double step = 1;
        int nsteps = 1;
        
        String[] object_names = new String[]{
                     "SSBar", "Mercu", "Venus", "EMB  ", "Mars ",
                     "Jupit", "Satur", "Uranu", "Neptu", "Pluto",
                     "Moon " };
        
        while (nsteps-->0) {
            System.out.println("JD: "+jd);
            
            double[] vectos = new double[6];
            for(int i = 0; i < 11; i++) {
                if( i == 10) {
                    fis.pleph(jd, JPLEphTarget.EARTH, JPLEphTarget.MOON, vectos, true);
                    vectos[0] *= AU_IN_KM;
                    vectos[1] *= AU_IN_KM;
                    vectos[2] *= AU_IN_KM;
                } else {
                    fis.pleph(jd, 
                            (i>0)?JPLEphTarget.values()[i]:JPLEphTarget.SUN, 
                            JPLEphTarget.SSBARY, vectos, true);
                }
                System.out.print(object_names[i]+" ");
                System.out.print(vectos[0]+" ");
                System.out.print(vectos[1]+" ");
                System.out.print(vectos[2]+"\n");
            }
            
            jd += step;
        }
        
        fis.close();
        
    }
    
}
