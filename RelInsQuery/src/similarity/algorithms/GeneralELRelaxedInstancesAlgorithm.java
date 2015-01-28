package similarity.algorithms;

import interpretation.ds.CanonicalInterpretation;
import interpretation.generator.CanonicalInterpretationGenerator;

import java.util.Collections;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

import similarity.algorithms.specifications.WeightedInputSpecification;
import util.ConsolePrinter;

public class GeneralELRelaxedInstancesAlgorithm implements
		IRelaxedInstancesAlgorithm<WeightedInputSpecification> {

	private CanonicalInterpretation m_TBoxModel;
	
	private CanonicalInterpretation m_KBModel;
	
	@Override
	public Set<OWLNamedIndividual> relaxedInstances(
			WeightedInputSpecification specification) {
		if(!specification.isValid()){
			System.err.println("No valid input specification.");
			System.exit(1);
		}
		
		CanonicalInterpretationGenerator generator = new CanonicalInterpretationGenerator(); // KB mode first
		m_KBModel = generator.generate(specification.getOntology());
		
		generator = new CanonicalInterpretationGenerator(specification.getQuery()); // TBox mode second, it alters the TBox
		m_TBoxModel = generator.generate(specification.getOntology());
		
		// show everything so far (incl. flattened ontology in order to know what the intermediaries are defined as)
		ConsolePrinter.printOntology(specification.getOntology());
		ConsolePrinter.SEP();
		ConsolePrinter.printInterpretation(m_KBModel);
		ConsolePrinter.SEP();
		ConsolePrinter.printInterpretation(m_TBoxModel);
		ConsolePrinter.SEP();
		System.out.println(specification.getQuery());
		
		// TODO compute relaxed instances
		
		return Collections.emptySet();
	}

}
