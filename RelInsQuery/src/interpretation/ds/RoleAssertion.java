package interpretation.ds;

import org.semanticweb.owlapi.model.OWLObjectProperty;

/**
 * For canonical models, it is not necessary to store all role successor relations
 * to be accessed seperately, the successor relations are stored implicitly in the 
 * domain nodes. This object structure is only created upon retrieving a role and 
 * is kept alive only as long as it is needed somewhere.
 * 
 * @author Maximilian Pensel
 *
 */
public class RoleAssertion {

	private DomainNode<?> m_from;
	private DomainNode<?> m_to;
	
	private PointedInterpretation m_toPointed;
	
	private OWLObjectProperty m_property;
	
	public RoleAssertion(DomainNode<?> from, DomainNode<?> to, OWLObjectProperty property) {
		m_from = from;
		m_to = to;
		m_property = property;
	}
	
	public RoleAssertion(DomainNode<?> from, DomainNode<?> to, OWLObjectProperty property, CanonicalInterpretation pointed) {
		m_from = from;
		m_to = to;
		m_property = property;
		if(pointed != null)
			m_toPointed = new PointedInterpretation(pointed, to);
	}
	
	public DomainNode<?> getFrom() {
		return m_from;
	}
	
	public OWLObjectProperty getProperty() {
		return m_property;
	}
	
	public DomainNode<?> getTo() {
		return m_to;
	}
}

