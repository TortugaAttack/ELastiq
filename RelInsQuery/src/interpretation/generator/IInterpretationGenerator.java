package interpretation.generator;

import org.semanticweb.owlapi.model.OWLOntology;

import owl.OntologyOperator;

import interpretation.ds.IDomain;
import interpretation.ds.OntologyInterpretation;

public interface IInterpretationGenerator {

	public OntologyInterpretation generate(OWLOntology ontology);
	
}
