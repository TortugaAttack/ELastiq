package interpretation.ds;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;

public class DomainNode<T> {
	
	private T m_id;
	
	private Set<OWLClass> m_instantiators;
	private HashMap<OWLObjectProperty, Set<DomainNode<?>>> m_successors;
	
	public DomainNode() {
		this(null);
	}
	
	public DomainNode(T id) {
		m_id = id;
	}
	
	public boolean addInstantiator(OWLClass A){
		return m_instantiators.add(A);
	}
	
	public boolean addSuccessor(OWLObjectProperty r, DomainNode<?> d){
		if(!m_successors.containsKey(r)){
			m_successors.put(r, new HashSet<DomainNode<?>>());
		}
		return m_successors.get(r).add(d);
	}
	
	public T getId(){
		return m_id;
	}
	
	@Override
	public String toString() {
		return m_id.toString();
	}
}
