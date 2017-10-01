package org.tu_dresden.elastiq.similarity.algorithms.generalEL;

import org.tu_dresden.elastiq.interpretation.ds.PointedInterpretation;

public class SimilarityValue {

	private PointedInterpretation m_p1;
	private PointedInterpretation m_p2;
	
	private int m_currentIteration;
	
	private double m_evenValue;

	private double m_oddValue;
	
	protected SimilarityValue(PointedInterpretation p1, PointedInterpretation p2) {
		// only accept creation in iteration 0
		m_currentIteration = 0;
		m_evenValue = 0.0;
		m_oddValue = -1; // negative similarity: red flag
		
		m_p1 = p1;
		m_p2 = p2;
	}
	
	public int getCurrentIteration() {
		return m_currentIteration;
	}
	
	public double getValue(int iteration) {
		if(iteration % 2 == 0){
			return m_evenValue;
		}else {
			return m_oddValue;
		}
	}
	
	public void setNewValue(double v){
		m_currentIteration++;
		if(m_currentIteration % 2 == 0){ // if the new iteration is an even one, set the even value
			m_evenValue = v;
		}else {
			m_oddValue = v;
		}
		
//		SimilarityValueFactory.getFactory().pushUpdate(this);
	}
	
	public PointedInterpretation getP1() {
		return m_p1;
	}
	
	public PointedInterpretation getP2() {
		return m_p2;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("msim(");
		sb.append(m_p1 + ", ");
		sb.append(m_p2+ ", ");
		sb.append(m_currentIteration + ") = ");
		if(m_currentIteration % 2 == 0)
			sb.append(m_evenValue + "\t//\t");
		else
			sb.append(m_oddValue + "\t//\t");
		
		sb.append("msim(");
		sb.append(m_p1 + ", ");
		sb.append(m_p2+ ", ");
		sb.append((m_currentIteration-1) + ") = ");
		if(m_currentIteration % 2 == 0)
			sb.append(m_evenValue);
		else
			sb.append(m_oddValue);
		
		return sb.toString();
	}
}
