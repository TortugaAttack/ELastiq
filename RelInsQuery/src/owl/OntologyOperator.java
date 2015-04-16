package owl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import main.StaticValues;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owl.transform.OWLOntologyTransformer;
import owl.transform.flatten.OWLAxiomFlatteningTransformer;
import tracker.TimeTracker;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl;

public class OntologyOperator implements IOWLOntologyExtension {

	private static Map<OWLOntology, OntologyOperator> m_operators;
	
	private OWLOntology m_ontology;
	
	private OWLAxiomFlatteningTransformer m_flattener;
	
	private OWLReasoner m_reasoner;
	
	private boolean m_ontologyChanged;
	
	private boolean m_isFlat;
	
	private OntologyOperator(OWLOntology o) {
		this.m_ontology = o;
		this.m_isFlat = false;
		this.m_ontologyChanged = true;
	}
	
	public static OntologyOperator getOntologyOperator(OWLOntology o){
		if(m_operators == null) m_operators = new HashMap<OWLOntology, OntologyOperator>();
		if(!m_operators.containsKey(o)) m_operators.put(o, new OntologyOperator(o));
		return m_operators.get(o);
	}
	
	public void flatten(){
		if(m_flattener == null)
			m_flattener = new OWLAxiomFlatteningTransformer();
		m_flattener.transform(m_ontology);
		this.m_isFlat = true;
	}
	
	public boolean isFlat() {
		return m_isFlat;
	}
	
	public void process(InferenceType type){
		if(m_reasoner == null){
			m_reasoner = new ElkReasonerFactory().createReasoner(m_ontology);
		}
		
		if(m_ontologyChanged || !m_reasoner.isPrecomputed(type)){
			TimeTracker.getInstance().start(StaticValues.TIME_REASONING);
			m_reasoner.flush();
			m_reasoner.precomputeInferences(type);
			m_ontologyChanged = false;
			TimeTracker.getInstance().stop(StaticValues.TIME_REASONING);
		}
	}
	
	public OWLReasoner getReasoner() {
		return getReasoner(false);
	}
	
	public OWLReasoner getReasoner(boolean includeABox) {
		if(includeABox)
			process(InferenceType.CLASS_ASSERTIONS);
		else
			process(InferenceType.CLASS_HIERARCHY);
		
		return m_reasoner;
	}
	
	public OWLAxiomFlatteningTransformer getFlatteningTransformer(){
//		if(!isFlat() || m_ontologyChanged)
		if(!isFlat())
			flatten();
		
		return m_flattener;
	}
	
	public OWLClassExpression getDefinition(OWLClass c){
		Set<OWLEquivalentClassesAxiom> axioms = m_ontology.getEquivalentClassesAxioms(c);
		if(axioms.size() > 1)
			System.out.println("warning multiple definitions found");
		if(axioms.size() == 0)
			System.out.println("warning no definitions found");
		for(OWLAxiom ax : axioms){
			for(OWLClassExpression expr : ((OWLEquivalentClassesAxiom)ax).getClassExpressions()){
				if(!expr.equals(c)) return expr;
			}
		}
		return null;
	}
	
	public OWLOntology getOntology() {
		return m_ontology;
	}
	
	public void ontologyChanged(){
		this.m_ontologyChanged = true;
	}
}
