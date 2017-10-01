package org.tu_dresden.elastiq.interpretation.generator;

import java.util.logging.Logger;

import org.semanticweb.owlapi.model.OWLClassExpression;

public class InterpretationGeneratorFactory {

	/**
	 * By default creates and returns an IterativeKBDomainElementGenerator
	 * with on-the-fly normalization.
	 * @return 
	 */
	public static IInterpretationGenerator create(){
		return create(true);
	}
	
	public static IInterpretationGenerator create(boolean normalize){
		return create(normalize, Logger.getLogger("InterpretationGeneratorLogger"));
	}
	
	public static IInterpretationGenerator create(boolean normalize, Logger log){
		IInterpretationGenerator gen = new IterativeKBInterpretationGenerator(normalize);
		if(log != null)
			gen.setLogger(log);
		
		return gen;
	}
	
	
	public static IInterpretationGenerator create(OWLClassExpression query){
		return create(query, true);
	}
	
	public static IInterpretationGenerator create(OWLClassExpression query, boolean normalize){
		return create(query, normalize, Logger.getLogger("InterpretationGeneratorLogger"));
	}
	
	public static IInterpretationGenerator create(OWLClassExpression query, boolean normalize, Logger log){
		IInterpretationGenerator gen = new IterativeQTBoxModelGenerator(query, normalize);
		if(log != null)
			gen.setLogger(log);
		
		return gen;
	}
}
