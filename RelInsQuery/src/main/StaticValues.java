package main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;
import org.semanticweb.owlapi.profiles.UseOfNonAbsoluteIRI;
import org.semanticweb.owlapi.profiles.UseOfUndeclaredClass;
import org.semanticweb.owlapi.profiles.UseOfUndeclaredDataProperty;
import org.semanticweb.owlapi.profiles.UseOfUndeclaredObjectProperty;

import similarity.algorithms.specifications.OutputType;

public class StaticValues {

	
	/* **************** names and output files ***************** */
	public static final String LOGGER_NAME = "relins-logger";
	public static final String LOGGER_FILE = "sim.log";
	
	public static final String ELK_LOGGER_NAME = "org.semanticweb.elk";
	public static String ELK_LOG_FILE = "elk.log";
	
	private static final Map<OutputType, String> DEFAULT_OUTPUT_FILES = new HashMap<OutputType, String>();
	public static String getDefaultOutputFile(OutputType t){
		if(DEFAULT_OUTPUT_FILES.isEmpty()){
			DEFAULT_OUTPUT_FILES.put(OutputType.ASCII, "value_development.txt");
			DEFAULT_OUTPUT_FILES.put(OutputType.CSV, "value_development.csv");
			DEFAULT_OUTPUT_FILES.put(OutputType.INSTANCES, "answers.txt");
			DEFAULT_OUTPUT_FILES.put(OutputType.STATISTICS, "statistics.csv");
			DEFAULT_OUTPUT_FILES.put(OutputType.TIMES, "times.txt");
		}
		return DEFAULT_OUTPUT_FILES.get(t);
	}
	/* ********************* code block descriptors for time tracking *************** */
	public static final String TIME_TOTAL = "total computation";
	// preprocessing time blocks
	public static final String TIME_PREPROCESSING = "preprocessing";
	public static final String TIME_PROFILE_CHECK = "checking EL profile";
	public static final String TIME_INPUT_PARSING = "input initialization";
	public static final String TIME_FLATTENING = "flattening";
	public static final String TIME_MODEL_KB = "KB model generation";
	public static final String TIME_MODEL_QT = "QTBox model generation";
	public static final String TIME_EXPLICIT_SUCCESSORS = "adding role-assertion successors";
	public static final String TIME_DIRECT_TBOX_SUCCESSORS = "adding direct TBox-successors";
	public static final String TIME_RECURSIVE_TBOX_SUCCESSORS = "recursively adding TBox successors";
	public static final String TIME_SMALL_MODEL = "instance exists check";
	public static final String TIME_DOMAIN_ELEMENTS_CREATION = "domain elements generation";
	public static final String TIME_INDIVIDUAL_DOMAIN_ELEM = "individual domain elements";
	public static final String TIME_ER_DOMAIN_ELEM = "existential restriction domain elements";
	public static final String TIME_ADD_INSTANTIATORS = "adding instantiators";
	public static final String TIME_DOMAIN_SUCCESSORS = "adding domain element successors";
	public static final String TIME_TBOX_SUCCESSORS = "TBox successors";
	public static final String TIME_KB_SUCCESSORS = "KB successors";
	public static final String TIME_REASONING = "elk reasoning";
	// query answering time blocks
	public static final String TIME_MAIN_ALGO = "query answering";
	public static final String TIME_ITERATION = "iteration";
	
	/* ************ statistically tracked values ***************** */
	public static final String STAT_ASSOCIATION_CONTAINS_CHECKS = "class association contains checks";
	public static final String STAT_ASSOCIATIONS = "class associations with individuals";
	public static final String STAT_KB_MODEL_SIZE = "KB model domain nodes";
	public static final String STAT_QT_MODEL_SIZE = "QTBox model domain nodes";
	
	/* *********************** DL profile stuff ***************** */
	public static final OWLProfile REQUIRED_PROFILE = new OWL2ELProfile();
	
	private static Set<Class<? extends OWLProfileViolation>> IGNORED_PROFILE_VIOLATIONS;
	private static void initIgnoredViolations(){
		IGNORED_PROFILE_VIOLATIONS = new HashSet<Class<? extends OWLProfileViolation>>();
		IGNORED_PROFILE_VIOLATIONS.add(UseOfUndeclaredClass.class);
		IGNORED_PROFILE_VIOLATIONS.add(UseOfUndeclaredObjectProperty.class);
		IGNORED_PROFILE_VIOLATIONS.add(UseOfNonAbsoluteIRI.class);
		IGNORED_PROFILE_VIOLATIONS.add(UseOfUndeclaredDataProperty.class);
	}
	public static boolean isViolationIgnored(OWLProfileViolation violation){
		if(IGNORED_PROFILE_VIOLATIONS == null) initIgnoredViolations();
		return IGNORED_PROFILE_VIOLATIONS.contains(violation.getClass());
	}
	
	/* *************************** miscellaneous ********************** */
	public static final String SEPERATOR = "------------------------------------------";
	
	public static final String ENTITY_REGEX = "[a-zA-Z0-9_\\-\\.]+";
	
	public static final String DOUBLE_0_1_REGEX = "(0(\\.[0-9]+){0,1}|1)";
}

