package similarity.algorithms.generalEL;

import interpretation.ds.PointedInterpretation;

public class SimilarityValue {

	private PointedInterpretation m_p1;
	private PointedInterpretation m_p2;
	
	private int m_currentIteration;
	
	private double m_currentValue;

	private double m_previousValue;
	
	protected SimilarityValue(PointedInterpretation p1, PointedInterpretation p2) {
		// only accept creation in iteration 0
		m_currentIteration = 0;
		m_currentValue = 0.0;
		m_previousValue = -1; // negative similarity: red flag
		
		m_p1 = p1;
		m_p2 = p2;
	}
	
	public int getCurrentIteration() {
		return m_currentIteration;
	}
	
	public double getValue(int iteration) {
		if(iteration == m_currentIteration){
			return m_currentValue;
		}else if(iteration == m_currentIteration - 1){
			return m_previousValue;
		}else{
			return -1; // red flag
		}
	}
	
	public void setNewValue(double v){
		m_currentIteration++;
		m_previousValue = m_currentValue;
		m_currentValue = v;
		
		SimilarityValueFactory.getFactory().pushUpdate(this);
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
		sb.append(m_currentValue + "\t//\t");
		
		sb.append("msim(");
		sb.append(m_p1 + ", ");
		sb.append(m_p2+ ", ");
		sb.append((m_currentIteration-1) + ") = ");
		sb.append(m_previousValue);
		
		return sb.toString();
	}
}
