package Main;

import interpretation.ds.CanonicalDomain;
import interpretation.ds.OntologyInterpretation;
import interpretation.generator.CanonicalInterpretationGenerator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import de.uulm.ecs.ai.owlapi.krssparser.KRSS2OntologyFormat;

import owl.OntologyOperator;
import owl.io.OWLOntologyLoader;
import owl.io.OntologyFileSimplifier;
import owl.transform.flatten.OWLAxiomFlatteningTransformer;

public class Main {
	
	private static OWLOntologyManager MANAGER;

	public static void main(String[] args) {
		MANAGER = OWLManager.createOWLOntologyManager();
		OWLOntologyLoader loader = new OWLOntologyLoader(MANAGER);
		OWLOntology o = loader.load("exPaper01.ofn");
//		OWLOntology o = loader.load("ex02NotNormalized.ofn");
		
		OntologyOperator ontOp = OntologyOperator.getOntologyOperator(o);
		try{
		
			printOntology(o,false);
//			ontOp.flatten();
//			System.out.println(StaticValues.SEPERATOR);
//			printOntology(o, false);
			
			// create a specific query concept
			OWLDataFactory df = MANAGER.getOWLDataFactory();
			OWLClassExpression query = df.getOWLObjectIntersectionOf(
					df.getOWLClass(IRI.create("A")),
					df.getOWLObjectSomeValuesFrom(
							df.getOWLObjectProperty(IRI.create("s")),
							df.getOWLObjectIntersectionOf(
									df.getOWLClass(IRI.create("B")),
									df.getOWLClass(IRI.create("C"))
									))
					);
			
			CanonicalInterpretationGenerator generator = new CanonicalInterpretationGenerator(); // KB mode first
			OntologyInterpretation iKB = generator.generate(o);
			
			generator = new CanonicalInterpretationGenerator(query); // TBox mode last, it alters the TBox
			OntologyInterpretation iQT = generator.generate(o);
			
			// show flat ontology first
			System.out.println(StaticValues.SEPERATOR);
			printOntology(ontOp.getOntology(), false);
			
			// show interpretations
			System.out.println(StaticValues.SEPERATOR);
			System.out.println("I_Q,T");
			System.out.println(iQT);
			System.out.println(StaticValues.SEPERATOR);
			System.out.println("I_KB");
			System.out.println(iKB);
		
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void printOntology(OWLOntology o){
		System.out.println("TBox:");
		for(OWLAxiom ax : o.getTBoxAxioms(true)){
			System.out.println(ax.toString());
		}
		System.out.println("ABox:");
		for(OWLAxiom ax : o.getABoxAxioms(true)){
			System.out.println(ax.toString());
		}
	}
	
	/**
	 * Prints a simplified version of te ontology by saving it to a temporary file
	 * @param o
	 * @param keepFile
	 */
	public static void printOntology(OWLOntology o, boolean keepFile){
		try {
			
			File tmp = File.createTempFile("ont", "krss");
			
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			man.saveOntology(o, new OWLFunctionalSyntaxOntologyFormat(), IRI.create(tmp));
			
			OntologyFileSimplifier s = new OntologyFileSimplifier();
			Map<String,String> customRegex = new HashMap<String, String>();
//			customRegex.put("OntologyID\\(Anonymous-[0-9]*\\)#", "");
//			customRegex.put("\\>", "");
//			customRegex.put("\\<", "");
//			customRegex.put("Prefix.*", "");
			
			System.out.print(s.simplify(tmp, customRegex));

			if(!keepFile){
				tmp.delete();
			}
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static OWLOntologyManager getOntologyManager(){
		return MANAGER;
	}
}
