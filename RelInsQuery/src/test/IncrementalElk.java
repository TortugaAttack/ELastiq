package test;

import java.io.File;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import Main.StaticValues;

import owl.io.OWLOntologyLoader;

public class IncrementalElk {

	
	public static void main(String[] args) throws OWLOntologyCreationException {

//		OWLOntology o = new OWLOntologyLoader().load("exIncr.ofn");
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology o = man.loadOntologyFromOntologyDocument(new File("exIncr.ofn"));
		
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(o);
		
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		
		System.out.println(StaticValues.SEPERATOR);
		
		printHirarchy(reasoner);
		
		System.out.println(StaticValues.SEPERATOR);
		
		OWLDataFactory df = OWLManager.getOWLDataFactory();
		OWLClassExpression cj = df.getOWLObjectIntersectionOf(df.getOWLClass(IRI.create("C")),
															  df.getOWLClass(IRI.create("A")));
		AddAxiom add = new AddAxiom(o, df.getOWLSubClassOfAxiom(df.getOWLClass(IRI.create("CJ")), cj));
		man.applyChange(add);
		
		reasoner.flush();
		
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		
		printHirarchy(reasoner);
		
	}
	
	private static void printHirarchy(OWLReasoner reasoner){
		System.out.println("Indirect Superclasses:");
		for(OWLClass c : reasoner.getRootOntology().getClassesInSignature()){
			System.out.println(c + " subs by " + reasoner.getSuperClasses(c, false).getFlattened());
		}
		
		System.out.println("Equivalent Classes:");
		for(OWLClass c : reasoner.getRootOntology().getClassesInSignature()){
			System.out.println(c + " subs by " + reasoner.getEquivalentClasses(c));
		}
	}
}
