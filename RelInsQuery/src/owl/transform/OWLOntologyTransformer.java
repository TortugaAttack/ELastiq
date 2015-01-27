package owl.transform;

import org.semanticweb.owlapi.model.OWLOntology;

public interface OWLOntologyTransformer {

	/**
	 * Applies a transformation routine to an OWLOntology.
	 * This routine should already apply possible changes to the ontology
	 * (instead of returning change instructions).
	 * 
	 * @param o
	 */
	public void transform(OWLOntology o);
	
}
