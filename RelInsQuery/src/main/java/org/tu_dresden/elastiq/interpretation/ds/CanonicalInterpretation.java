package org.tu_dresden.elastiq.interpretation.ds;

import org.semanticweb.owlapi.util.ShortFormProvider;

/**
 * 
 * This represents a general interpretation.
 * How the domain elements are organized can be varied by
 * using different IDomain objects.
 * 
 * So far no explicit sets for the interpretation mapping are set,
 * but if they should be required, they belong here.
 * 
 * For canonical models, the domain elements are structured by a graph,
 * thus most of the information regarding instantiator classes or roles are
 * expressed by the DomainNode elements, which are specially organized in the
 * CanonicalDomain.
 * 
 * @author Maximilian Pensel
 * maximilian.pensel@mailbox.tu-dresden.de
 *
 */
public class CanonicalInterpretation {

	private String m_name;
	
	private CanonicalDomain m_domain;
	
	private ShortFormProvider m_shortFormProvider;
	
	private static int cnt = 1;
	
	// private FunctionMapping m_mapping; // there was no need to have a generic implementation of the function mapping so far
	
	public CanonicalInterpretation() {
		this("I" + cnt++);
	}
	
	public CanonicalInterpretation(String name){
		m_name = name;
	}
	
	/**
	 * This is used to specify the type of domain that some InterpretationGenerator requires.
	 * @param d
	 */
	public void initDomain(CanonicalDomain d){
		m_domain = d;
	}
	
	public CanonicalDomain getDomain(){
		return m_domain;
	}
	
	public void setName(String name){
		this.m_name = name;
	}
	
	public String getName(){
		return this.m_name;
	}
	
	public void setShortFormProvider(ShortFormProvider shortFormProvider) {
		this.m_shortFormProvider = shortFormProvider;
		m_domain.setShortFormProvider(m_shortFormProvider);
	}
	
	@Override
	public String toString() {
		return m_domain.toString();
	}
}
