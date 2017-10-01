package org.tu_dresden.elastiq.interpretation.generator;

import java.util.logging.Logger;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.tu_dresden.elastiq.interpretation.ds.CanonicalInterpretation;

public interface IInterpretationGenerator {

	public CanonicalInterpretation generate(OWLOntology ontology);
	
	public void setSmallCreationFlag(boolean small);
	
	/**
	 * Initialize your own logger for the generator.
	 * If not actively specified, default logger will be used.
	 * Call with null to disable logging for the generator. 
	 * @param log
	 */
	public void setLogger(Logger LOG);
	
	public OWLClass getClassRepresentation(OWLClassExpression expr);
}
