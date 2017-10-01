package org.tu_dresden.elastiq.util;

public class EasyTimes {

	public static String niceTime(long millis){
		System.out.println(millis);
		int min = (int)(millis/(1000*60)); // floored
		int sec = (int)((millis - (min*1000*60)) / 1000);
		if(min == 0 && sec == 0){
			return millis + " ms";
		}
		String minS = min+"";
		if(minS.length() < 2) minS = "0" + minS;
		String secS = sec+"";
		if(secS.length() < 2) secS = "0" + secS;
		return minS + ":" + secS + " min";
	}
}
