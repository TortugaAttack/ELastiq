package interpretation.ds;

import java.util.HashMap;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public class CanonicalDomain implements IDomain {

	/**
	 * Stores domain elements associated with class expressions.
	 */
	private HashMap<OWLClassExpression, DomainNode<OWLClassExpression>> m_conceptElements;
	
	/**
	 * Stores domain elements associated with individuals.
	 */
	private HashMap<OWLIndividual, DomainNode<OWLIndividual>> m_individualElements;
	
	public CanonicalDomain() {
		m_conceptElements = new HashMap<OWLClassExpression, DomainNode<OWLClassExpression>>();
		m_individualElements = new HashMap<OWLIndividual, DomainNode<OWLIndividual>>();
	}
	
	@Override
	public DomainNode<?> addDomainElement(Object id) {
		if(id instanceof OWLClassExpression){
			return addDomainElement((OWLClassExpression)id);
		}
		
		if(id instanceof OWLIndividual){
			return addDomainElement((OWLIndividual)id);
		}
		
		return null;
	}
	
	/**
	 * Create and store a new domain element, if the id is unused.
	 * @param expr - domain element id, an OWLClassExpression
	 * @return true if the element was actually added, false if an element with that id already exists
	 */
	private DomainNode<OWLClassExpression> addDomainElement(OWLClassExpression expr){
		if(!m_conceptElements.containsKey(expr)){
			m_conceptElements.put(expr, new DomainNode<OWLClassExpression>(expr));
		}
		
		return m_conceptElements.get(expr);
	}
	
	/**
	 * Create and store a new domain element, if the id is unused.
	 * @param expr - domain element id, an OWLIndividual
	 * @return true if the element was actually added, false if an element with that id already exists
	 */
	private DomainNode<OWLIndividual> addDomainElement(OWLIndividual ind){
		if(!m_individualElements.containsKey(ind)){
			m_individualElements.put(ind, new DomainNode<OWLIndividual>(ind));
		}
		
		return m_individualElements.get(ind);
	}
	
	/**
	 * Fetches the DomainNode associated with the given OWLClassExpression.
	 * Does NOT create a domain element if none exists for the given identifier.
	 * @param c
	 * @param expr
	 * @return true if the instantiator has actually been added
	 */
//	public boolean addElementInstantiator(OWLClass c, OWLClassExpression expr){
//		if(m_conceptElements.containsKey(expr)){
//			m_conceptElements.get(expr).addInstantiator(c);
//		}
//		return false;
//	}
	
	/**
	 * Fetches the DomainNode associated with the given OWLIndividual.
	 * Does NOT create a domain element if none exists for the given identifier.
	 * @param c
	 * @param ind
	 * @return true if the instantiator has actually been added
	 */
//	public boolean addElementInstantiator(OWLClass c, OWLIndividual ind){
//		if(m_individualElements.containsKey(ind)){
//			m_individualElements.get(ind).addInstantiator(c);
//		}
//		return false;
//	}
	
	/**
	 * Simply passes the instantiator through to the given domain element
	 * @param c
	 * @param d
	 * @return true if the instantiator has actually been added
	 */
//	public boolean addElementInstantiator(OWLClass c, DomainNode<?> d){
//		return d.addInstantiator(c);
//	}
	
	/**
	 * For now adding successors directly between DomainNode elements suffices.
	 * ToDo: implements all variants of adding successor relations between OWLClassExpression ids and
	 * OWLIndividual ids.
	 * 
	 * Will be done directly by the generator, since he is traversing DomainNode elements anyway
	 * @param r
	 * @param from
	 * @param to
	 * @return true if the relation was actually added
	 */
//	public boolean addSuccessorRelation(OWLObjectProperty r, DomainNode<?> from, DomainNode<?> to){
//		return from.addSuccessor(r, to);
//	}
	
	public HashMap<OWLClassExpression, DomainNode<OWLClassExpression>> getConceptElements() {
		return m_conceptElements;
	}
	
	public HashMap<OWLIndividual, DomainNode<OWLIndividual>> getIndividualElements() {
		return m_individualElements;
	}
	
	public DomainNode<OWLClassExpression> getDomainNode(OWLClassExpression expr){
		return m_conceptElements.get(expr);
	}
	
	public DomainNode<OWLIndividual> getDomainNode(OWLIndividual ind){
		return m_individualElements.get(ind);
	}
}
