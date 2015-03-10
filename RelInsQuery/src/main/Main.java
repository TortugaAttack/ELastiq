package main;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import main.log.BasicLogFormatter;
import main.log.CustomConsoleHandler;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;

import owl.io.OWLOntologyLoader;
import owl.transform.el.OWLToELTransformer;

import similarity.algorithms.generalEL.GeneralELOutputGenerator;
import similarity.algorithms.generalEL.GeneralELRelaxedInstancesAlgorithm;
import similarity.algorithms.specifications.BasicInputSpecification;
import similarity.algorithms.specifications.GeneralParameters;
import similarity.algorithms.specifications.IInputSpecification;
import similarity.algorithms.specifications.WeightedInputSpecification;
import similarity.algorithms.specifications.parser.WeightedInputSpecificationParser;
import statistics.StatStore;
import tracker.BlockOutputMode;
import tracker.TimeTracker;
import util.EasyTimes;

public class Main {	
	
	private static OWLOntologyManager MANAGER;
	
	private static BasicInputSpecification INPUT;
	
	private static TimeTracker TRACKER = TimeTracker.getInstance();

	private static final void finish(){
		TimeTracker.getInstance().stopAll();
		Logger.getLogger(StaticValues.LOGGER_NAME).info(TimeTracker.getInstance().createEvaluation());
		GeneralELOutputGenerator gen = new GeneralELOutputGenerator(null, INPUT);
		gen.storeOutputs((File)INPUT.getParameters().getValue(GeneralParameters.OUT_DIR));
	}
	
	public static void main(String[] args) {
		
		/* *** TESTING STUFF **** */
//		if(args.length < 1){
//			System.err.println("No ontology file given.");
//			System.exit(1);
//		}
//		
//		File dir = new File(args[0]);
//		System.out.println("Searching " + dir.getPath());
//		
//		for(File f : dir.listFiles(new FilenameFilter() {
//			@Override
//			public boolean accept(File dir, String name) {
//				return name.endsWith(".owl");
//			}
//		})){
//			System.out.println("Handling " + f.getName());
//
//			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
//			OWLOntologyLoader l = new OWLOntologyLoader(man);
//			OWLOntology o = l.load(f);
//			if(o != null){
//			OWLToELTransformer t = new OWLToELTransformer();
//			t.transform(o);
//			
//			File out = new File(dir.getPath() + "/" + f.getName());
//			System.out.println("Saving to " + out.getPath());
//			l.save(out, o, new OWLFunctionalSyntaxOntologyFormat());
//			
//			man.removeOntology(o);
//			}
//		}	
//		System.exit(1);
		/* *** TEST DONE **** */
		Thread hook = new Thread(){
			@Override
			public void run() {
				finish();
			}			
		};
		Runtime.getRuntime().addShutdownHook(hook); // in case the computation is interrupted prematurely
		
		TRACKER.setDisplayDepth(10);
		TRACKER.start(StaticValues.TIME_TOTAL, BlockOutputMode.IN_TREE);
		if(args.length < 1){
			System.err.println("No specification file given.");
			System.exit(1);
		}
		Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
		
		TRACKER.start(StaticValues.TIME_INPUT_PARSING, BlockOutputMode.IN_TREE);
		WeightedInputSpecificationParser parser = new WeightedInputSpecificationParser(new File(args[0]));
		INPUT = new WeightedInputSpecification();
		try{
			INPUT = parser.parse();
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
		if(INPUT.getQueries().isEmpty()){
			LOG.severe("None of the given queries where parsable.");
			finish();
			System.exit(1);
		}
		TRACKER.stop(StaticValues.TIME_INPUT_PARSING);
		
		TRACKER.start(StaticValues.TIME_PREPROCESSING, BlockOutputMode.IN_TREE);
		StatStore.getInstance().enterValue("TBox Axioms", INPUT.getOntology().getTBoxAxioms(true).size() * 1.0);
		StatStore.getInstance().enterValue("ABox Axioms", INPUT.getOntology().getABoxAxioms(true).size() * 1.0);
		StatStore.getInstance().enterValue("Individuals", INPUT.getOntology().getIndividualsInSignature(true).size() * 1.0);
//		OWLOntologyLoader loader = new OWLOntologyLoader(INPUT.getOntology().getOWLOntologyManager());
//		loader.save(new File("examples/snomed2010a_alt.ofn"), INPUT.getOntology(), new OWLFunctionalSyntaxOntologyFormat());
//		System.exit(1);
		
		// setup logging (after spec-parsing, log-level may depend on specification
		String logFile = ((File)INPUT.getParameters().getValue(GeneralParameters.OUT_DIR)).getAbsolutePath();
		if(!logFile.endsWith("/")) logFile += "/";
		setupLogging(logFile + StaticValues.LOGGER_FILE, (Level)INPUT.getParameters().getValue(GeneralParameters.LOG_LEVEL));
		
		// optional:
		int i = 1;
		for(OWLClassExpression query : INPUT.getQueries()){
			LOG.fine("Query " + i + ": " + query);
			i++;
		}
		
		TRACKER.start(StaticValues.TIME_PROFILE_CHECK);
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
		TRACKER.stop(StaticValues.TIME_PROFILE_CHECK);
		// create the algorithm (if additional algorithm e.g. for unfoldable TBoxes exists, have a process here
		// to decide the type of algorithm depending on the ontology structure)
		GeneralELRelaxedInstancesAlgorithm algo = new GeneralELRelaxedInstancesAlgorithm();

		Map<Integer, Map<OWLNamedIndividual, Double>> answers = algo.relaxedInstances((WeightedInputSpecification)INPUT);
		
		GeneralELOutputGenerator outGenerator = new GeneralELOutputGenerator(algo, INPUT);
		
		for(int query = 1; query <= INPUT.getQueries().size(); query++){
			String resultMsg = "Query " + query + ": " + answers.get(query).size() + " elements have a similarity greater than " + INPUT.getThreshold() + " to " + INPUT.getQueries().get(query-1) + "\n";
			if(LOG.getLevel() == Level.INFO || LOG.getLevel() == Level.FINE){
				resultMsg += outGenerator.renderInstanceList(query);
			}
			LOG.info(resultMsg);
		}
		
		// the following 3 lines would be executed from the shutdown hook anyway, could omit removing hook
		// only difference is that the completed algorithm is present in the output generator
		TRACKER.stopAll();
		LOG.info("Time tracking results:\n" + TRACKER.createEvaluation());
		
		outGenerator.storeOutputs((File)INPUT.getParameters().getValue(GeneralParameters.OUT_DIR));
		
		Runtime.getRuntime().removeShutdownHook(hook);
	}
	
	private static void setupLogging(String logFile, Level level){
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
		
		// take care of the **** elk log4j logger
		org.apache.log4j.Logger elkLogger = org.apache.log4j.Logger.getRootLogger();
		elkLogger.removeAllAppenders();
		
		elkLogger.setLevel(org.apache.log4j.Level.toLevel(level.toString()));

		// somehow the file appender removes contents from file in the end
		FileAppender fa = new FileAppender();
		fa.setLayout(new PatternLayout("%-5r [%t] %-5p %c %x - %m%n"));
		if(logFile.contains("/")){
			fa.setFile(logFile.substring(0, logFile.lastIndexOf("/")+1) + StaticValues.ELK_LOG_FILE);
		}else{
			fa.setFile(StaticValues.ELK_LOG_FILE);
		}
		fa.setAppend(true);
		fa.setThreshold(org.apache.log4j.Level.toLevel(level.toString()));
		fa.activateOptions();
		
		ConsoleAppender ca = new ConsoleAppender();
		ca.setLayout(new PatternLayout("%-5r [%t] %-5p %c %x - %m%n"));
		ca.setThreshold(org.apache.log4j.Level.toLevel(level.toString()));
		ca.activateOptions();
		
		elkLogger.addAppender(fa);
		elkLogger.addAppender(ca);
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
