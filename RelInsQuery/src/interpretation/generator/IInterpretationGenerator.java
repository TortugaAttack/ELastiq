package interpretation.generator;

import org.semanticweb.owlapi.model.OWLOntology;

import owl.OntologyOperator;

import interpretation.ds.IDomain;
import interpretation.ds.CanonicalInterpretation;

public interface IInterpretationGenerator {

	public CanonicalInterpretation generate(OWLOntology ontology);
	
}
