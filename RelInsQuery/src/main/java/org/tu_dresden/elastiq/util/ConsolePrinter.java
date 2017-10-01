package org.tu_dresden.elastiq.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.tu_dresden.elastiq.interpretation.ds.CanonicalInterpretation;
import org.tu_dresden.elastiq.main.StaticValues;
import org.tu_dresden.elastiq.owl.io.OntologyFileSimplifier;

public class ConsolePrinter {
	
	public static void SEP(){
		System.out.println(StaticValues.SEPERATOR);
	}
	
	public static void printInterpretation(CanonicalInterpretation interpretation){
		System.out.println(interpretation); // maybe some more output required..
	}
	
	public static String getOntologyString(OWLOntology o){
		StringBuilder sb = new StringBuilder();
		sb.append(o + "\n");
		sb.append("TBox:" + "\n");
		for(OWLAxiom ax : o.getTBoxAxioms(true)){
			sb.append(ax.toString() + "\n");
		}
		sb.append("ABox:" + "\n");
		for(OWLAxiom ax : o.getABoxAxioms(true)){
			sb.append(ax.toString() + "\n");
		}
		return sb.toString();
	}
	
	public static void printOntology(OWLOntology o){
		System.out.println(getOntologyString(o));
	}
	
	/**
	 * Prints a simplified version of the ontology by saving it to a temporary file
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

}
