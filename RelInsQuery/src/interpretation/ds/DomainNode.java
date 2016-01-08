package interpretation.ds;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import statistics.StatStore;

/**
 * Generic type DomainNode, identification of domain nodes may vary between use-cases.
 * 
 * @author Maximilian Pensel - maximilian.pensel@gmx.de
 *
 * @param <T> : Specifies the type for the identifier of the domain node.
 */
public class DomainNode<T> {
	
	private T m_id;
	
	private Set<OWLClass> m_instantiators;
	
	private HashMap<OWLObjectProperty, Set<DomainNode<?>>> m_successors;
	
	/**
	 * Creates the domain node with a given identifier.
	 * @param id : The generic identifier object.
	 */
	public DomainNode(T id) {
		m_id = id;
		m_instantiators = new HashSet<OWLClass>();
		m_successors = new HashMap<OWLObjectProperty, Set<DomainNode<?>>>();
	}
	
	/**
	 * Add an instantiator for this DomainNode, that is an OWLClass such that 
	 * @l
	 * @param A
	 * @return
	 */
	public boolean addInstantiator(OWLClass A){
		return m_instantiators.add(A);
	}
	
	public void removeInstantiator(OWLClass A){
		m_instantiators.remove(A);
	}
	
	public void removeInstantiators(Set<OWLClass> As){
		m_instantiators.removeAll(As);
	}
	
	public boolean addSuccessor(OWLObjectProperty r, DomainNode<?> d){
		if(!m_successors.containsKey(r)){
			m_successors.put(r, new HashSet<DomainNode<?>>());
		}
		boolean ret = m_successors.get(r).add(d);
		if(m_id instanceof OWLClassExpression){
			StatStore.getInstance().enterValue("successors added for CE domNode", m_successors.get(r).size()*1.0);
		}else{
			StatStore.getInstance().enterValue("successors added for ind domNode", m_successors.get(r).size()*1.0);
		}
		return ret;
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
	/**
	 * Compile RoleConnection set for all successors through the given role
	 * without pointed interpretation.
	 * @param r
	 * @return
	 */
	public Set<RoleConnection> getSuccessorObjects(OWLObjectProperty r){
		return getSuccessorObjects(r, null);
	}
	
	/**
	 * Compile RoleConnection set for all successors through the given role.
	 * @param r
	 * @param c
	 * @return
	 */
	public Set<RoleConnection> getSuccessorObjects(OWLObjectProperty r, CanonicalInterpretation c){
		Set<RoleConnection> successors = new HashSet<RoleConnection>();
		for(DomainNode<?> to : m_successors.get(r)){
			successors.add(new RoleConnection(this, to, r, c));
		}
		return successors;
	}
	/**
	 * Compile RoleConnection set for all role successors without a pointed interpretation.
	 * @return
	 */
	public Set<RoleConnection> getSuccessorObjects(){
		return getSuccessorObjects((CanonicalInterpretation)null);
	}
	
	/**
	 * Compile RoleConnection set for all role successors.
	 * If no canonical interpretation is given, the returned roles will still contain the destination node.
	 * @param c
	 * @return
	 */
	public Set<RoleConnection> getSuccessorObjects(CanonicalInterpretation c){
		Set<RoleConnection> successors = new HashSet<RoleConnection>();
		for(OWLObjectProperty r : m_successors.keySet()){
			for(DomainNode<?> to : m_successors.get(r)){
				successors.add(new RoleConnection(this, to, r, c));
			}
		}
		StatStore.getInstance().enterValue("successor set sizes ever returned", successors.size()*1.0);
		return successors;
	}
	
	public T getId(){
		return m_id;
	}
	
	public String toShortString(){
		return "d(" + m_id.toString() + ")";
	}
	
	@Override
	public int hashCode() {
		return m_id.hashCode();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.toShortString() + "  ");
		
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
