package owl.transform.el;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.util.OWLObjectVisitorExAdapter;

public class OWLToELVisitor extends OWLObjectVisitorExAdapter<OWLClassExpression>{

	private OWLOntology m_ontology;
	private OWLOntologyManager m_manager;
	
	private int m_removedAxioms;
	private int m_removedCEs;
	
	@Override
	public OWLClassExpression visit(OWLOntology ontology) {
		m_removedCEs = 0;
		m_removedAxioms = 0;
		m_ontology = ontology;
		m_manager = m_ontology.getOWLOntologyManager();
//		for(OWLAxiom ax : ontology.getTBoxAxioms(false)){
//			ax.accept(this);
//		}
		
		for(OWLAxiom ax : ontology.getRBoxAxioms(false)){
			ax.accept(this);
		}
		return null;
	}
	
	@Override
	public OWLClassExpression visit(OWLSubClassOfAxiom axiom) {
		OWLClassExpression sub = axiom.getSubClass();
		OWLClassExpression sup = axiom.getSuperClass();
		
		OWLClassExpression subNew = sub.accept(this);
		OWLClassExpression supNew = sup.accept(this);
		
		if(!sub.equals(subNew) || !sup.equals(supNew)){
			m_manager.removeAxiom(m_ontology, axiom);
			OWLDataFactory df = m_manager.getOWLDataFactory();
			m_manager.addAxiom(m_ontology, df.getOWLSubClassOfAxiom(subNew, supNew));
		}
		
		return null; 
	}
	
	@Override
	public OWLClassExpression visit(OWLEquivalentClassesAxiom axiom) {
		Set<OWLClassExpression> expressions = new HashSet<OWLClassExpression>();
		boolean changed = false;
		for(OWLClassExpression ex : axiom.getClassExpressions()){
			OWLClassExpression exNew = ex.accept(this);
			if(!ex.equals(exNew)){
				changed = true;
			}
			expressions.add(exNew);
		}
		if(changed){
			m_manager.removeAxiom(m_ontology, axiom);
			OWLDataFactory df = m_manager.getOWLDataFactory();
			m_manager.addAxiom(m_ontology, df.getOWLEquivalentClassesAxiom(expressions));
		}
		
		return null;
	}
	
	@Override
	public OWLClassExpression visit(OWLSubObjectPropertyOfAxiom axiom) {
		m_removedAxioms++;
		m_manager.removeAxiom(m_ontology, axiom);
		return null;
	}
	
	@Override
	public OWLClassExpression visit(OWLInverseObjectPropertiesAxiom axiom) {
		m_removedAxioms++;
		m_manager.removeAxiom(m_ontology, axiom);
		return null;
	}
	
	@Override
	public OWLClassExpression visit(OWLFunctionalObjectPropertyAxiom axiom) {
		m_removedAxioms++;
		m_manager.removeAxiom(m_ontology, axiom);
		return null;
	}
	
	@Override
	public OWLClassExpression visit(OWLObjectIntersectionOf desc) {
		Set<OWLClassExpression> expressions = new HashSet<OWLClassExpression>();
		boolean changed = false;
		for(OWLClassExpression ex : desc.getOperands()){
			OWLClassExpression exNew = ex.accept(this);
			if(!ex.equals(exNew)){
				changed = true;
				if(exNew != null){
					expressions.add(exNew);
				}
			}else{
				expressions.add(ex);
			}
		}
		if(changed){
			if(expressions.size() == 0){
				return null;
			}else if(expressions.size() == 1){
				return expressions.iterator().next();
			}else{
				return m_manager.getOWLDataFactory().getOWLObjectIntersectionOf(expressions);
			}
		}
		
		return desc;
	}
	
	@Override
	public OWLClassExpression visit(OWLObjectSomeValuesFrom desc) {
		OWLClassExpression exNew = desc.getFiller().accept(this);
		if(!desc.getFiller().equals(exNew)){
			if(exNew == null) return null; // remove the entire restriction
			OWLDataFactory df = m_manager.getOWLDataFactory();
			return df.getOWLObjectSomeValuesFrom(desc.getProperty(), exNew);
		}
		return desc;
	}
	
	@Override
	public OWLClassExpression visit(OWLClass desc) {
		return desc;
	}
	
	@Override
	public OWLClassExpression visit(OWLDataHasValue desc) {
		m_removedCEs++;
		return null;
	}
	
	public int getRemovedCEs() {
		return m_removedCEs;
	}
	
	public int getRemovedAxioms() {
		return m_removedAxioms;
	}
	
}
