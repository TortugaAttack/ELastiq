package interpretation.ds;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owl.IOWLOntologyExtension;
import owl.OntologyOperator;

public class DomainNode<T> {
	
	private T m_id;
	
	private Set<OWLClass> m_instantiators;
	private HashMap<OWLObjectProperty, Set<DomainNode<?>>> m_successors;
	
	public DomainNode() {
		this(null);
	}
	
	public DomainNode(T id) {
		m_id = id;
		m_instantiators = new HashSet<OWLClass>();
		m_successors = new HashMap<OWLObjectProperty, Set<DomainNode<?>>>();
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
	
	public void removeSuccessor(OWLObjectProperty r, DomainNode<?> d){
		if(m_successors.containsKey(r)){
			m_successors.get(r).remove(d);
			if(m_successors.get(r).isEmpty())
				m_successors.remove(r);
		}
	}
	
	public Set<OWLClass> getInstantiators() {
		return m_instantiators;
	}
	
	public Set<OWLObjectProperty> getSuccessorRoles(){
		return m_successors.keySet();
	}
	
	public Set<DomainNode<?>> getSuccessors(OWLObjectProperty r){
		if(m_successors.containsKey(r))
			return m_successors.get(r);
		return Collections.emptySet();
	}
	
	public T getId(){
		return m_id;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("d(" + m_id.toString() + ")  ");
		
		// instantiators
		sb.append("[");
		Iterator<OWLClass> cIt = m_instantiators.iterator();
		while(cIt.hasNext()){
			sb.append(cIt.next().toString());
			if(cIt.hasNext())
				sb.append(", ");
		}
		sb.append("]  ");
		
		// successors
		sb.append("[");
		Iterator<OWLObjectProperty> rIt = m_successors.keySet().iterator();
		while(rIt.hasNext()){
			OWLObjectProperty role = rIt.next();
			sb.append(role.toString() + "{");
			Iterator<DomainNode<?>> dIt = m_successors.get(role).iterator();
			while(dIt.hasNext()){
				sb.append("d(" + dIt.next().getId().toString() + ")");
				if(dIt.hasNext())
					sb.append(", ");
			}
			sb.append("}");
			if(rIt.hasNext())
				sb.append(", ");
		}
		sb.append("]  ");
		
		return sb.toString();
	}
}
