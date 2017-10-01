package org.tu_dresden.elastiq.owl.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.tu_dresden.elastiq.main.Main;
import org.tu_dresden.elastiq.similarity.algorithms.specifications.BasicInputSpecification;




/**
 * Tries to parse a query with a potential assortment of query parsers.
 * 
 * @author Maximilian Pensel
 *
 */
public class OWLQueryParser {

	private List<OWLQueryParser> m_parsers;
	
	private OWLOntology m_target;
	
	public OWLQueryParser() {
//		System.err.println("The cake is a lie.");
//		System.exit(1);
	}
	
	public OWLQueryParser(OWLOntology o) {
		m_parsers = new ArrayList<OWLQueryParser>();
		m_target = o;
	}
	
	public OWLClassExpression parse(String classExpression) throws OWLParserException{
		if(m_parsers.isEmpty()) registerDefault();
		Map<Class<? extends OWLQueryParser>, String> errMsgs = new HashMap<Class<? extends OWLQueryParser>, String>();
		for(OWLQueryParser p : m_parsers){
			try{
				return p.parse(classExpression);
			}catch(Exception e){
				errMsgs.put(p.getClass(), e.getMessage());
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Query not parsable:\n");
		for(Class<? extends OWLQueryParser> clazz : errMsgs.keySet()){
			sb.append(clazz + ": " + errMsgs.get(clazz) + "\n");
		}
		throw new OWLParserException(sb.toString());
	}
	
	public void registerParser(OWLQueryParser p){
		if(p instanceof OWLQueryParser) return; // do not accept nested parser registrations
		m_parsers.add(p);
	}
	
	private void registerDefault(){
		if(Main.getInputs() instanceof BasicInputSpecification){
			m_parsers.add(new SimpleManchesterOWLQueryParser(m_target));
		}
	}
}
