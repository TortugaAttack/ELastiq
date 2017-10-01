package org.tu_dresden.elastiq.owl.transform.flatten;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.tu_dresden.elastiq.main.Main;
import org.tu_dresden.elastiq.main.StaticValues;
import org.tu_dresden.elastiq.owl.transform.OWLOntologyTransformer;

import tracker.BlockOutputMode;
import tracker.TimeTracker;

public class OWLAxiomFlatteningTransformer implements OWLOntologyTransformer{

	private OWLAxiomFlatteningVisitor m_visitor;
	
	private static final String NAMESPACE_TEMPLATE = "flat#:";
	
	private String m_namespace;
	
	@Override
	public void transform(OWLOntology o) {
		TimeTracker.getInstance().start(StaticValues.TIME_FLATTENING, BlockOutputMode.IN_TREE);
		HashSet<String> namespaces = new HashSet<String>();
		for(OWLOntology os : o.getImportsClosure()){
			if(os.getOntologyID() != null && os.getOntologyID().getOntologyIRI() != null){
				if(!os.getOntologyID().getOntologyIRI().getNamespace().isEmpty())
					namespaces.add(os.getOntologyID().getOntologyIRI().getNamespace());
			}
		}
		m_namespace = NAMESPACE_TEMPLATE.replace("#", "");
		int i = 1;
		while(namespaces.contains(m_namespace)){
			m_namespace = NAMESPACE_TEMPLATE.replace("#", i+"");
			i++;
		}
		
		if(m_visitor == null)
			m_visitor = new OWLAxiomFlatteningVisitor(m_namespace);
		o.accept(m_visitor);
		
		Main.getOntologyManager().applyChanges(m_visitor.getChanges());
		m_visitor.resetChangeList();
		TimeTracker.getInstance().stop(StaticValues.TIME_FLATTENING);
	}
	
//	public OWLClassExpression transform(OWLClassExpression expr){
//		if(m_visitor == null)
//			m_visitor = new OWLAxiomFlatteningVisitor();
//		OWLClassExpression newExpr = expr.accept(m_visitor);
//		
//		OWLManager.createOWLOntologyManager().applyChanges(m_visitor.getChanges());
//		m_visitor.resetChangeList();
//		
//		return newExpr;
//	}
	
	public Set<OWLObjectSomeValuesFrom> getRestrictions(){
		return m_visitor.getIntroducedRestrictionDefinitions().keySet();
	}
	
	public OWLClass getIntermediary(OWLClassExpression ce){
		if(ce instanceof OWLClass) return (OWLClass)ce;
		if(m_visitor.getIntroducedConjunctionDefinitions().containsKey(ce)){
			return m_visitor.getIntroducedConjunctionDefinitions().get(ce);
		}
		
		// will return null if no intermediary was introduced for ce
		return m_visitor.getIntroducedRestrictionDefinitions().get(ce);
	}

	
	public boolean isIntermediary(OWLClass c){
		return m_namespace.equals(c.getIRI().getNamespace());
//		return m_visitor.getIntroducedRestrictionDefinitions().containsValue(c)
//				|| m_visitor.getIntroducedConjunctionDefinitions().containsValue(c);
	}
	
	public OWLAxiomFlatteningVisitor getVisitor(){
		return m_visitor; 
	}
}
