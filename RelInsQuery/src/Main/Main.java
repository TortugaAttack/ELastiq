package Main;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.CanonicalInterpretation;
import interpretation.generator.CanonicalInterpretationGenerator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxClassExpressionParser;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import de.uulm.ecs.ai.owlapi.krssparser.KRSS2OntologyFormat;

import owl.OntologyOperator;
import owl.io.OWLOntologyLoader;
import owl.io.OWLQueryParser;
import owl.io.OntologyFileSimplifier;
import owl.transform.flatten.OWLAxiomFlatteningTransformer;
import similarity.algorithms.GeneralELRelaxedInstancesAlgorithm;
import similarity.algorithms.specifications.IInputSpecification;
import similarity.algorithms.specifications.WeightedInputSpecification;
import similarity.measures.entities.DefaultEntitySimilarityMeasure;
import uk.ac.manchester.cs.owlapi.dlsyntax.parser.DLSyntaxParser;

public class Main {
	
	private static OWLOntologyManager MANAGER;
	
	private static IInputSpecification INPUT;

	public static void main(String[] args) {
		// usually read parameter to obtain input specification file
		WeightedInputSpecification in = new WeightedInputSpecification();
		INPUT = in;
		
		in.setOntologyFile("examples/ex03-paper01.ofn");
		in.setQuery("A AND s SOME (B AND C)");
		in.setPrimitiveMeasure(new DefaultEntitySimilarityMeasure());
		in.setDiscountingFactor(0.8);
		in.setThreshold(0.5);
		
		GeneralELRelaxedInstancesAlgorithm algo = new GeneralELRelaxedInstancesAlgorithm();
		
		algo.relaxedInstances(in);
	}
	
	public static OWLOntologyManager getOntologyManager(){
		if(MANAGER == null)
			MANAGER = OWLManager.createOWLOntologyManager();
		return MANAGER;
	}
	
	public static IInputSpecification getInputs(){
		return INPUT;
	}
}
