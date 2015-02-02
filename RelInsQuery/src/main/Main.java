package main;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.CanonicalInterpretation;
import interpretation.generator.CanonicalInterpretationGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import main.log.BasicLogFormatter;
import main.log.CustomConsoleHandler;

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
import org.semanticweb.owlapi.model.OWLNamedIndividual;
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
import similarity.algorithms.generalEL.GeneralELRelaxedInstancesAlgorithm;
import similarity.algorithms.specifications.BasicInputSpecification;
import similarity.algorithms.specifications.GeneralParameters;
import similarity.algorithms.specifications.IInputSpecification;
import similarity.algorithms.specifications.OutputType;
import similarity.algorithms.specifications.TerminationMethod;
import similarity.algorithms.specifications.WeightedInputSpecification;
import similarity.algorithms.specifications.parser.WeightedInputSpecificationParser;
import similarity.measures.entities.DefaultEntitySimilarityMeasure;
import similarity.measures.entities.PrimitiveEntitySimilarityMeasure;
import uk.ac.manchester.cs.owlapi.dlsyntax.parser.DLSyntaxParser;

public class Main {
	
	private static OWLOntologyManager MANAGER;
	
	private static BasicInputSpecification INPUT;

	public static void main(String[] args) {
		if(args.length < 1){
			System.err.println("No specification file given.");
			System.exit(1);
		}
		Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
		
		// usually read parameter to obtain input specification file
//		WeightedInputSpecification in = new WeightedInputSpecification();
//		INPUT = in;
		WeightedInputSpecificationParser parser = new WeightedInputSpecificationParser(new File(args[0]));
		INPUT = new WeightedInputSpecification();
		try{
			INPUT = parser.parse();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
		// setup logging
//		in.getParameters().enterValue(GeneralParameters.LOG_LEVEL, Level.FINE);
		setupLogging("spec_base", (Level)INPUT.getParameters().getValue(GeneralParameters.LOG_LEVEL));
		
		// set main inputs
//		in.setOntologyFile("examples/ex03-paper01.ofn");
//		in.setQuery("A AND s SOME (B AND C)");
//		in.setPrimitiveMeasure(new PrimitiveEntitySimilarityMeasure());
//		in.setDiscountingFactor(0.8);
//		in.setThreshold(0.1);
		
//		in.setWeight(MANAGER.getOWLDataFactory().getOWLClass(IRI.create("A")), 0.1);
//		in.setWeight(MANAGER.getOWLDataFactory().getOWLObjectProperty(IRI.create("s")), 0.4);
//		in.setDefaultWeight(0.5);
//		in.setWeight(MANAGER.getOWLDataFactory().getOWLClass(IRI.create("C")), 1);
		
//		in.setTerminationMethod(TerminationMethod.RELATIVE, 0.00005);
//		in.setTerminationMethod(TerminationMethod.ABSOLUTE, 12);
		
//		OWLDataFactory f = OWLManager.getOWLDataFactory();
//		((PrimitiveEntitySimilarityMeasure)in.getPrimitiveMeasure()).registerSimilarity(f.getOWLClass(IRI.create("Low")), f.getOWLClass(IRI.create("Medium")), 0.5);
//		((PrimitiveEntitySimilarityMeasure)in.getPrimitiveMeasure()).registerSimilarity(f.getOWLClass(IRI.create("High")), f.getOWLClass(IRI.create("Medium")), 0.5);
//		
//		in.getParameters().addOutput(OutputType.ASCII);
		
		
//		LOG.setLevel((Level)in.getParameters().getValue(GeneralParameters.LOG_LEVEL));
		
		GeneralELRelaxedInstancesAlgorithm algo = new GeneralELRelaxedInstancesAlgorithm();

		
		Map<OWLNamedIndividual, Double> answers = algo.relaxedInstances((WeightedInputSpecification)INPUT);
		String resultMsg = answers.size() + " elements have a similarity greater than " + INPUT.getThreshold() + " to " + INPUT.getQuery() + "\n";
		if(LOG.getLevel() == Level.INFO || LOG.getLevel() == Level.FINE){
			for(OWLNamedIndividual ind : answers.keySet()){
				resultMsg += ind + "\tsimilarity: " + answers.get(ind) + "\n";
			}
		}
		LOG.info(resultMsg);
	}
	
	private static void setupLogging(String spec, Level level){
		Logger.getLogger("org.semanticweb.elk").setLevel(level);
		Logger log = Logger.getLogger(StaticValues.LOGGER_NAME);
		
		log.setLevel(level);
		log.setUseParentHandlers(false);
//		ConsoleHandler consoleLogHandler = new ConsoleHandler();
//		consoleLogHandler.setFormatter(new BasicLogFormatter(true));
		CustomConsoleHandler consoleLogHandler = new CustomConsoleHandler();
		consoleLogHandler.setLevel(level);
		log.addHandler(consoleLogHandler);
		try{
			String logFile = StaticValues.LOGGER_FILE;
			if(spec != null) logFile = logFile.replace("@", spec);
			FileHandler fileLogHandler = new FileHandler(logFile);
			fileLogHandler.setFormatter(new BasicLogFormatter(false));
			fileLogHandler.setLevel(level);
			log.addHandler(fileLogHandler);
		}catch(IOException ioe){
			System.err.println(ioe.getMessage());
		}
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
