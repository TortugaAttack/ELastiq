package main;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;
import org.semanticweb.owlapi.profiles.UseOfNonAbsoluteIRI;
import org.semanticweb.owlapi.profiles.UseOfUndeclaredClass;
import org.semanticweb.owlapi.profiles.UseOfUndeclaredObjectProperty;

import com.sun.org.apache.xml.internal.security.Init;

public class StaticValues {

	
	public static final String LOGGER_NAME = "relins-logger";
	
	public static final String LOGGER_FILE = "sim.log";
	
	public static final OWLProfile REQUIRED_PROFILE = new OWL2ELProfile();
	
	public static final String SEPERATOR = "------------------------------------------";
	
	public static int DECIMAL_ACCURACY = 10;
	
	public static final String ENTITY_REGEX = "[a-zA-Z0-9_\\-\\.]+";
	
	public static final String DOUBLE_0_1_REGEX = "(0(\\.[0-9]+){0,1}|1)";
	
	private static Set<Class<? extends OWLProfileViolation>> IGNORED_PROFILE_VIOLATIONS;
	
	private static void initIgnoredViolations(){
		IGNORED_PROFILE_VIOLATIONS = new HashSet<Class<? extends OWLProfileViolation>>();
		IGNORED_PROFILE_VIOLATIONS.add(UseOfUndeclaredClass.class);
		IGNORED_PROFILE_VIOLATIONS.add(UseOfUndeclaredObjectProperty.class);
		IGNORED_PROFILE_VIOLATIONS.add(UseOfNonAbsoluteIRI.class);
	}
	
	public static boolean isViolationIgnored(OWLProfileViolation violation){
		if(IGNORED_PROFILE_VIOLATIONS == null) initIgnoredViolations();
		return IGNORED_PROFILE_VIOLATIONS.contains(violation.getClass());
	}
}

