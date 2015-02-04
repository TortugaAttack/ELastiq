package util;

public class EasyMath {
	
	public static double round(double value, int precision){
		double pot = Math.pow(10, precision);
		value = Math.round(value*pot)/pot;
		return value;
	}

}
