package interpretation.ds;

import org.semanticweb.owlapi.model.OWLOntology;

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
public class OntologyInterpretation {

	private IDomain m_domain;
	
	private OWLOntology m_targetOntology;
	
	public OntologyInterpretation(OWLOntology o) {
		this.m_targetOntology = o;
	}
	
	/**
	 * This is used to specify the type of domain that some InterpretationGenerator requires.
	 * @param d
	 */
	public void initDomain(IDomain d){
		m_domain = d;
	}
	
	public IDomain getDomain(){
		return m_domain;
	}
}
