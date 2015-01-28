package owl.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.model.OWLClassExpression;

import similarity.algorithms.specifications.BasicInputSpecification;

import Main.Main;



/**
 * Tries to parse a query with a potential assortment of query parsers.
 * 
 * @author Maximilian Pensel
 *
 */
public class OWLQueryParser {

	private List<OWLQueryParser> m_parsers;
	
	public OWLQueryParser() {
		m_parsers = new ArrayList<OWLQueryParser>();
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
			m_parsers.add(new SimpleManchesterOWLQueryParser(
				((BasicInputSpecification)Main.getInputs()).getOntology()));
		}
	}
}
