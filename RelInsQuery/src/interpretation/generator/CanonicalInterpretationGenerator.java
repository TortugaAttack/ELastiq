package interpretation.generator;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.IDomain;
import interpretation.ds.OntologyInterpretation;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class CanonicalInterpretationGenerator implements IInterpretationGenerator {

	private OWLClassExpression m_referenceExpression;
	
	public CanonicalInterpretationGenerator() {
		this(null);
	}
	
	public CanonicalInterpretationGenerator(OWLClassExpression expr) {
		this.m_referenceExpression = expr;
	}
	
	@Override
	public OntologyInterpretation generate(OWLOntology o) {
		OntologyInterpretation canonInterpretation = new OntologyInterpretation(o);
		CanonicalDomain canonDomain = new CanonicalDomain();
		canonInterpretation.initDomain(canonDomain);
		
		
		
		for(OWLAxiom ax : o.getTBoxAxioms(true)){
			if(ax instanceof OWLSubClassOfAxiom){
				OWLSubClassOfAxiom subAx = (OWLSubClassOfAxiom)ax;
				if(subAx.getSuperClass() instanceof OWLObjectSomeValuesFrom){
					// TODO: if the TBox is flat (ABox?) we can read exists r.A expressions directly from the top-level of the axiom sides
				}
			}
		}
		
		
		return canonInterpretation;
	}

	
	
	private boolean isKBMode(){
		return m_referenceExpression == null;
	}
}
