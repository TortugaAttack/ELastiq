package interpretation.ds;

public class PointedInterpretation {

	private DomainNode<?> m_element;
	
	private CanonicalInterpretation m_interpretation;
	
	public PointedInterpretation(CanonicalInterpretation interpretation, DomainNode<?> elem) {
		m_interpretation = interpretation;
		m_element = elem;
	}
	
	public DomainNode<?> getElement() {
		return m_element;
	}
	
	public CanonicalInterpretation getInterpretation() {
		return m_interpretation;
	}
	
	@Override
	public String toString() {
		return "(" + m_interpretation.getName() + ", " + m_element.toShortString() + ")";
	}
	
	@Override
	public int hashCode() {
		return m_element.hashCode() * m_interpretation.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof PointedInterpretation)) return false;
		PointedInterpretation pObj = (PointedInterpretation)obj;
		return this.m_interpretation.equals(pObj.getInterpretation()) && this.m_element.equals(pObj.getElement());
	}
}
