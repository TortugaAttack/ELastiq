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
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import de.uulm.ecs.ai.owlapi.krssparser.KRSS2OntologyFormat;

import owl.OntologyOperator;
import owl.io.OWLOntologyLoader;
import owl.io.OWLQueryParser;
import owl.io.OntologyFileSimplifier;
import owl.transform.flatten.OWLAxiomFlatteningTransformer;
import similarity.algorithms.generalEL.GeneralELOutputGenerator;
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
		// setup logging (after spec-parsing, log-level may depend on specification
		String logFile = ((File)INPUT.getParameters().getValue(GeneralParameters.OUT_DIR)).getAbsolutePath();
		if(!logFile.endsWith("/")) logFile += "/";
		setupLogging(logFile + StaticValues.LOGGER_FILE, (Level)INPUT.getParameters().getValue(GeneralParameters.LOG_LEVEL));	
		
		// validate OWL Profile (The type of profile is fixed in StaticValues
		OWLProfileReport report = StaticValues.REQUIRED_PROFILE.checkOntology(INPUT.getOntology());
		if(!report.isInProfile()){
			boolean fatal = false;
			Map<Class<? extends OWLProfileViolation>, Integer> ignoredWarnings = new HashMap<Class<? extends OWLProfileViolation>, Integer>();
			for(OWLProfileViolation violation : report.getViolations()){
				if(StaticValues.isViolationIgnored(violation)){
					if(!ignoredWarnings.containsKey(violation.getClass()))
						ignoredWarnings.put(violation.getClass(), 0);
					ignoredWarnings.put(violation.getClass(), ignoredWarnings.get(violation.getClass())+1);
				}else{
					LOG.severe(violation.toString());
					fatal = true;
				}
			}
			for(Class<? extends OWLProfileViolation> violationClass : ignoredWarnings.keySet()){
				LOG.warning(ignoredWarnings.get(violationClass) + " profile violation of type " + violationClass.getSimpleName() + " were found but ignored.");
			}
			if(fatal)
				System.exit(1);
		}
		
		// create the algorithm (if additional algorithm e.g. for unfoldable TBoxes exists, have a process here
		// to decide the type of algorithm depending on the ontology structure)
		GeneralELRelaxedInstancesAlgorithm algo = new GeneralELRelaxedInstancesAlgorithm();

		Map<OWLNamedIndividual, Double> answers = algo.relaxedInstances((WeightedInputSpecification)INPUT);
		
		GeneralELOutputGenerator outGenerator = new GeneralELOutputGenerator(algo, INPUT);
		
		String resultMsg = answers.size() + " elements have a similarity greater than " + INPUT.getThreshold() + " to " + INPUT.getQuery() + "\n";
		if(LOG.getLevel() == Level.INFO || LOG.getLevel() == Level.FINE){
			resultMsg += outGenerator.renderInstanceList(answers);
		}
		LOG.info(resultMsg);
		
		outGenerator.storeOutputs((File)INPUT.getParameters().getValue(GeneralParameters.OUT_DIR));
	}
	
	private static void setupLogging(String logFile, Level level){
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
