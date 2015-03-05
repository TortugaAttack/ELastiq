package owl.transform.flatten;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import main.StaticValues;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomChange;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.util.OWLObjectVisitorExAdapter;


public class OWLAxiomFlatteningVisitor extends OWLObjectVisitorExAdapter<OWLClassExpression> {

	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	private int m_intermediaryRestrictionIndex;
	private int m_intermediaryConjunctionIndex;
	private String m_intermediaryRestrictionTemplate;
	private String m_intermediaryConjunctionTemplate;
	
	private List<OWLAxiomChange> m_changes;
	/**
	 * This is an accumulator-type used variable for recursion
	 */
	private int m_currentDepth;
	private OWLAxiom m_currenAxiom;
	
	
	private OWLOntology m_ontology;
	private OWLOntologyManager m_manager;
	
	private String m_namespace;
	
	/**
	 * Keeping track of the already newly introduced subclass axioms in order to avoid
	 * creating a new intermediary axiom twice.
	 */
	private HashMap<OWLObjectSomeValuesFrom, OWLClass> m_introducedRestrictionDefinitions;
	/**
	 * Keeping track of the already newly introduced equivalent classes axioms in order to avoid
	 * creating a new intermediary axiom twice.
	 */
	private HashMap<OWLObjectIntersectionOf, OWLClass> m_introducedConjunctionDefinitions;
	
	public OWLAxiomFlatteningVisitor() {
		this("");
	}
	
	public OWLAxiomFlatteningVisitor(String namespace) {
		m_intermediaryRestrictionIndex = 0;
		m_intermediaryConjunctionIndex = 0;
		
		this.m_manager = OWLManager.createOWLOntologyManager();
		
		m_changes = new LinkedList<OWLAxiomChange>();
		m_currentDepth = 0;
		m_currenAxiom = null;

		m_intermediaryRestrictionTemplate  = "ER#";
		m_intermediaryConjunctionTemplate  = "CJ#";
		
		m_introducedConjunctionDefinitions = new HashMap<OWLObjectIntersectionOf, OWLClass>();
		m_introducedRestrictionDefinitions = new HashMap<OWLObjectSomeValuesFrom, OWLClass>();
		
		m_namespace = namespace;
	}
	
	public IRI getNextIntermediary(boolean isConjunction){
		IRI nextIntermediary;
//		do{ // looping not necessary due to unique namespace
			if(isConjunction){
				nextIntermediary = IRI.create(/*m_ontology.getOntologyID()
						+ "#"
						+*/
						m_namespace +
						m_intermediaryConjunctionTemplate.replace("#", ""+(m_intermediaryConjunctionIndex)));
				m_intermediaryConjunctionIndex++;
			}else{
				nextIntermediary = IRI.create(/*m_ontology.getOntologyID()
						+ "#"
						+*/ 
						m_namespace +
						m_intermediaryRestrictionTemplate.replace("#", ""+(m_intermediaryRestrictionIndex)));
				m_intermediaryRestrictionIndex++;
			}
		// do as long as the created intermediary is already contained, in order to get a completely fresh intermediary
//		}while(m_ontology.containsClassInSignature(nextIntermediary));
		
		return nextIntermediary;
	}
	
	private boolean isIntermediary(OWLClass c){
		IRI iri = c.getIRI();
		return m_namespace.equals(iri.getNamespace());
//		return iri.toString().contains(m_intermediaryRestrictionTemplate.replace("#", "")) ||
//				iri.toString().contains(m_intermediaryConjunctionTemplate.replace("#", ""));
	}
	
	
	@Override
	public OWLClassExpression visit(OWLOntology ontology) {
		this.m_ontology = ontology;
		
		Set<OWLAxiom> tBox = ontology.getTBoxAxioms(true);
		// control variables for time tracking
		for(OWLAxiom ax : tBox){
			m_currenAxiom = ax;
			ax.accept(this);
		}
		Set<OWLAxiom> aBox = ontology.getABoxAxioms(true);
		for(OWLAxiom ax : aBox){
			ax.accept(this);
		}
		m_currentDepth = 0; // always leave the flattener with depth 0
		return null;
	}
	
	@Override
	public OWLClassExpression visit(OWLSubClassOfAxiom axiom) {
		m_currentDepth = 0;
		OWLClassExpression newCESub = axiom.getSubClass().accept(this);
		m_currentDepth = 0;
		OWLClassExpression newCESup = axiom.getSuperClass().accept(this);
		if(!newCESup.equals(axiom.getSuperClass()) || !newCESub.equals(axiom.getSubClass())){
			RemoveAxiom change1 = new RemoveAxiom(m_ontology, axiom);
			AddAxiom change2 = new AddAxiom(m_ontology,
					m_manager.getOWLDataFactory().getOWLSubClassOfAxiom(newCESub, newCESup));
			
			m_changes.add(change1);
			m_changes.add(change2);
		}
		
		return null;
	}
	
	@Override
	public OWLClassExpression visit(OWLEquivalentClassesAxiom axiom) {
		boolean hasChanged = false;
		m_currentDepth = 0;
		Set<OWLClassExpression> newCEs = new HashSet<OWLClassExpression>();
		for(OWLClassExpression ce : axiom.getClassExpressions()){
			OWLClassExpression newCE = ce.accept(this);
			
			newCEs.add(newCE); // in case ce did not change, we know ce == newCE 
			
			if(!hasChanged && !ce.equals(newCE)){
				hasChanged = true;
			}
		}
		
		if(hasChanged){
			RemoveAxiom change1 = new RemoveAxiom(m_ontology, axiom);
			AddAxiom change2 = new AddAxiom(m_ontology,
					m_manager.getOWLDataFactory().getOWLEquivalentClassesAxiom(newCEs));
			
			m_changes.add(change1);
			m_changes.add(change2);
		}
		
		return null;
	}
	
	public OWLClassExpression visit(OWLClassAssertionAxiom axiom){
		OWLClassExpression newCE = axiom.getClassExpression().accept(this);
		if(newCE != axiom.getClassExpression()){
			RemoveAxiom change1 = new RemoveAxiom(m_ontology, axiom);
			AddAxiom change2 = new AddAxiom(m_ontology,
					m_manager.getOWLDataFactory().getOWLClassAssertionAxiom(newCE, axiom.getIndividual()));
			
			m_changes.add(change1);
			m_changes.add(change2);
		}
		
		return null;
	}
	
	@Override
	public OWLClassExpression visit(OWLObjectIntersectionOf desc) {
		if(m_currentDepth == 0){
			Set<OWLClassExpression> exprs = new HashSet<OWLClassExpression>();
			m_currentDepth++;
			for(OWLClassExpression ex : desc.getOperands()){
				OWLClassExpression e = ex.accept(this);
				exprs.add(e);
			}
			return m_manager.getOWLDataFactory().getOWLObjectIntersectionOf(exprs);
		}else{
			Set<OWLClassExpression> exprs = new HashSet<OWLClassExpression>();
			m_currentDepth++;
			for(OWLClassExpression ex : desc.getOperands()){
				OWLClassExpression e = ex.accept(this);
				// e is flat now
				exprs.add(e);
			}
			m_currentDepth--;
			
			// create the new flat intersection
			OWLObjectIntersectionOf newDesc = m_manager.getOWLDataFactory().getOWLObjectIntersectionOf(exprs);
			
			// now search for an axiom that may already represent the newDesc
			// either it does not exist, then create a new axiom
			// or it exists, then simply return the respective defined class
			OWLClass c = null;
			c = m_introducedConjunctionDefinitions.get(newDesc);
			if(c == null){ // if no class with this definition exists, create one
				c = m_manager.getOWLDataFactory().getOWLClass(getNextIntermediary(true));
				
				OWLAxiom ax = m_manager.getOWLDataFactory().getOWLEquivalentClassesAxiom(c, newDesc);
				
				AddAxiom change = new AddAxiom(m_ontology, ax);
				m_changes.add(change);
				
				m_introducedConjunctionDefinitions.put(newDesc, c);
			}else{
				LOG.finest(c + " already existed");
			}
			
			return c;
		}
	}
	
	@Override
	public OWLClassExpression visit(OWLObjectSomeValuesFrom desc) {
		if(m_currentDepth == 0){
			OWLClassExpression expr = desc.getFiller();
			
			m_currentDepth++;
			
			OWLClassExpression newInnerExpr = expr.accept(this);
			
			OWLObjectSomeValuesFrom newDesc = m_manager.getOWLDataFactory().getOWLObjectSomeValuesFrom(desc.getProperty(), newInnerExpr);
			
			if(this.m_currenAxiom instanceof OWLSubClassOfAxiom){
				OWLClass c = m_introducedRestrictionDefinitions.get(newDesc); 
				if(c == null){
					c = m_manager.getOWLDataFactory()
							.getOWLClass(getNextIntermediary(false));
					
					m_changes.add(
							new AddAxiom(m_ontology, m_manager.getOWLDataFactory()
														.getOWLEquivalentClassesAxiom(c, newDesc)
								)
							);
					
					m_introducedRestrictionDefinitions.put(newDesc, c);
				}
				return c;
			}else{ // is equivalent classes axiom
				// figure out whether there is a non-intermediary equivalent class to the current restriction
				OWLClass c = null;
				for(OWLClassExpression eqex : ((OWLEquivalentClassesAxiom)m_currenAxiom).getClassExpressions()){
					if(eqex instanceof OWLClass && !isIntermediary((OWLClass)eqex)){
						c = (OWLClass)eqex; // found one
						break;
					}
				}
				
				if (c != null){
					m_introducedRestrictionDefinitions.put(newDesc, c);
					return newDesc;
				}else{
					OWLClass cintr = m_introducedRestrictionDefinitions.get(newDesc);
					if(cintr == null){
						cintr = m_manager.getOWLDataFactory()
								.getOWLClass(getNextIntermediary(false));
						
						m_changes.add(
								new AddAxiom(m_ontology, m_manager.getOWLDataFactory()
															.getOWLEquivalentClassesAxiom(cintr, newDesc)
									)
								);
						
						m_introducedRestrictionDefinitions.put(newDesc, cintr);
					}
					
					return cintr;
				}
			}
		}else{
			OWLClassExpression expr = desc.getFiller();
			m_currentDepth++;
			OWLClassExpression newInnerExpr = expr.accept(this);
			m_currentDepth--;
			
			// create the new flat existential restriction
			OWLObjectSomeValuesFrom newDesc = m_manager.getOWLDataFactory()
													.getOWLObjectSomeValuesFrom(desc.getProperty(), newInnerExpr);
			
			// now search for an axiom that may already represent the newDesc
			// either it does not exist, then create a new axiom
			// or it exists, then simply return the respective defined class
			OWLClass c = null;
			c = m_introducedRestrictionDefinitions.get(newDesc);
			if(c == null){ // if no class with this definition exists, create one
				c = m_manager.getOWLDataFactory().getOWLClass(getNextIntermediary(false));
				
				OWLAxiom ax = m_manager.getOWLDataFactory().getOWLEquivalentClassesAxiom(c, newDesc);
				
				AddAxiom change = new AddAxiom(m_ontology, ax);
				m_changes.add(change);
				// axiom does not need to be checked for flattening
				
				m_introducedRestrictionDefinitions.put(newDesc, c);
			}else{
				LOG.finest(c + " already existed");
			}
			
			return c;
		}
	}
	
	@Override
	public OWLClassExpression visit(OWLClass desc) {
		return desc;
	}
	
	public List<OWLAxiomChange> getChanges() {
		return m_changes;
	}
	
	public void resetChangeList(){
		m_changes = new ArrayList<OWLAxiomChange>();
	}
	
	public void setIntermediaryTemplate(String intermediaryTemplate) {
		if(intermediaryTemplate != null && intermediaryTemplate.contains("#")){
			this.m_intermediaryRestrictionTemplate = intermediaryTemplate;
		}else{
			System.err.println("The template must contain '#' to be replaced by the current index.\n"
					+ "The template stays at '" + this.m_intermediaryRestrictionTemplate + "'.");
		}
	}

	protected Map<OWLObjectIntersectionOf, OWLClass> getIntroducedConjunctionDefinitions() {
		return m_introducedConjunctionDefinitions;
	}
	
	protected Map<OWLObjectSomeValuesFrom, OWLClass> getIntroducedRestrictionDefinitions() {
		return m_introducedRestrictionDefinitions;
	}
}
