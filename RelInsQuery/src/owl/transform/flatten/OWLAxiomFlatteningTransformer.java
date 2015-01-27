package owl.transform.flatten;

import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;

import owl.transform.OWLOntologyTransformer;

public class OWLAxiomFlatteningTransformer implements OWLOntologyTransformer{

	private OWLAxiomFlatteningVisitor m_visitor;
	
	@Override
	public void transform(OWLOntology o) {
		m_visitor = new OWLAxiomFlatteningVisitor();
		o.accept(m_visitor);
		
		OWLManager.createOWLOntologyManager().applyChanges(m_visitor.getChanges());
		m_visitor.resetChangeList();
	}
	
	public OWLClassExpression transform(OWLClassExpression expr){
		if(m_visitor == null)
			m_visitor = new OWLAxiomFlatteningVisitor();
		OWLClassExpression newExpr = expr.accept(m_visitor);
		
		OWLManager.createOWLOntologyManager().applyChanges(m_visitor.getChanges());
		m_visitor.resetChangeList();
		
		return newExpr;
	}
	
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

}
