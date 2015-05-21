package interpretation.ds;

import java.util.HashMap;
import java.util.Map;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.util.ShortFormProvider;

public class CanonicalDomain implements IDomain {

	/**
	 * Stores domain elements associated with class expressions.
	 */
	private Map<OWLClassExpression, DomainNode<OWLClassExpression>> m_conceptElements;
	
	/**
	 * Stores domain elements associated with individuals.
	 */
	private Map<OWLNamedIndividual, DomainNode<OWLNamedIndividual>> m_individualElements;

	private ShortFormProvider m_shortFormProvider;
	
	public CanonicalDomain() {
		m_conceptElements = new HashMap<OWLClassExpression, DomainNode<OWLClassExpression>>();
		m_individualElements = new HashMap<OWLNamedIndividual, DomainNode<OWLNamedIndividual>>();
	}
	
	@Override
	public DomainNode<?> addDomainElement(Object id) {
		if(id instanceof OWLClassExpression){
			return addDomainElement((OWLClassExpression)id);
		}
		
		if(id instanceof OWLNamedIndividual){
			return addDomainElement((OWLNamedIndividual)id);
		}
		
		return null;
	}
	
	/**
	 * Create and store a new domain element, if the id is unused.
	 * @param expr - domain element id, an OWLClassExpression
	 * @return the associated domain element, no matter if it was newly created or already existed
	 */
	public DomainNode<OWLClassExpression> addDomainElement(OWLClassExpression expr){
		if(!m_conceptElements.containsKey(expr)){
			m_conceptElements.put(expr, new DomainNode<OWLClassExpression>(expr));
		}
		
		return m_conceptElements.get(expr);
	}
	
	/**
	 * Create and store a new domain element, if the id is unused.
	 * @param expr - domain element id, an OWLIndividual
	 * @return the associated domain element, no matter if it was newly created or already existed
	 */
	public DomainNode<OWLNamedIndividual> addDomainElement(OWLNamedIndividual ind){
		if(!m_individualElements.containsKey(ind)){
			m_individualElements.put(ind, new DomainNode<OWLNamedIndividual>(ind));
		}
		
		return m_individualElements.get(ind);
	}
	
	public void removeDomainNode(DomainNode<?> node){
		if(node.getId() instanceof OWLClassExpression){
			m_conceptElements.remove(node.getId());
		}else if(node.getId() instanceof OWLNamedIndividual){
			m_individualElements.remove(node.getId());
		}
	}
	
	public Map<OWLClassExpression, DomainNode<OWLClassExpression>> getConceptElements() {
		return m_conceptElements;
	}
	
	public Map<OWLNamedIndividual, DomainNode<OWLNamedIndividual>> getIndividualElements() {
		return m_individualElements;
	}
	
	public DomainNode<?> getDomainNode(Object id){
		if(id instanceof OWLClassExpression)
			return getDomainNode((OWLClassExpression)id);
		if(id instanceof OWLNamedIndividual)
			return getDomainNode((OWLNamedIndividual)id);
		
		return null;
	}
	
	public DomainNode<OWLClassExpression> getDomainNode(OWLClassExpression expr){
		return m_conceptElements.get(expr);
	}
	
	public DomainNode<OWLNamedIndividual> getDomainNode(OWLNamedIndividual ind){
		return m_individualElements.get(ind);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Concept Domain Elements:\n");
		for(OWLClassExpression ce : m_conceptElements.keySet()){
			sb.append(m_conceptElements.get(ce).toString() + "\n");
		}
		sb.append("\n");
		sb.append("Individual Domain Elements:\n");
		for(OWLIndividual ind : m_individualElements.keySet()){
			sb.append(m_individualElements.get(ind).toString() + "\n");
		}
		return sb.toString();
	}

	public Integer size() {
		return m_individualElements.size() + m_conceptElements.size();
	}
	
	public void setShortFormProvider(ShortFormProvider shortFormProvider) {
		this.m_shortFormProvider = shortFormProvider;
	}
}
