package interpretation.generator;

import org.semanticweb.owlapi.model.OWLOntology;

import interpretation.ds.OntologyInterpretation;

public interface IInterpretationGenerator {

	public OntologyInterpretation generate(OWLOntology o);
}
