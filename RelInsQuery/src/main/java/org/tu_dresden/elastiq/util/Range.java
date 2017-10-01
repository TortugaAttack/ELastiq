package org.tu_dresden.elastiq.util;

public class Range {

	private int m_lower;
	
	private int m_upper;
	
	public Range(int from, int to) {
		m_lower = Math.min(from, to);
		m_upper = Math.max(from, to);
	}
	
	public int getLower() {
		return m_lower;
	}
	
	public int getUpper() {
		return m_upper;
	}
	
	public int getRandom(){
		return EasyMath.random(m_lower, m_upper);
	}
}
