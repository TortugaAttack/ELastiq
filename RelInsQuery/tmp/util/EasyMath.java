package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EasyMath {
	
	public static double round(double value, int precision){
		double pot = Math.pow(10, precision);
		value = Math.round(value*pot)/pot;
		return value;
	}
	
	/**
	 * creates random values from a given value to another, including the values of the bounds.
	 * @param from lower bound
	 * @param to upper bound
	 * @return random integer within bounds [from, to]
	 */
	public static int random(int from, int to){
		if(to < from) throw new NumberFormatException("Illegal interval bounds, lower > upper.");
		return (int)(Math.random()*(to-from)) + from;
	}
	
	public static <T> T random(List<T> pool){
		int i = random(0, pool.size()-1);
		return pool.get(i);
	}
	
	public static <T> T random(List<T> pool, Collection<T> restriction){
		List<T> realPool = new ArrayList<T>();
		for(T t : pool){
			if(!restriction.contains(t)) realPool.add(t);
		}
		return random(realPool);
	}

}
