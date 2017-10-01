package org.tu_dresden.elastiq.owl.io;

import java.io.File;
import java.util.logging.Logger;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.tu_dresden.elastiq.main.StaticValues;



public class OWLOntologyLoader {

	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	private OWLOntologyManager m_manager;
	
	public OWLOntologyLoader(OWLOntologyManager manager) {
		m_manager = manager;
	}

	public OWLOntology load(String file){
		return load(new File(file));
	}
	
	public OWLOntology load(File file){
		try {
			OWLOntology o = m_manager.loadOntology(IRI.create(file));
			LOG.info("Done loading " + o);
			return o;
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void save(File f, OWLOntology o, OWLOntologyFormat format){
		try {
			m_manager.saveOntology(o, format, IRI.create(f));
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
