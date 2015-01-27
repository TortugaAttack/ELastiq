package owl.io;

import java.io.File;
import java.util.logging.Logger;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import Main.StaticValues;


public class OWLOntologyLoader {

	private static final Logger LOG = Logger.getLogger(StaticValues.LOGGER_NAME);
	
	public OWLOntology load(String file){
		return load(new File(file));
	}
	
	public OWLOntology load(File file){
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		try {
			OWLOntology o = man.loadOntology(IRI.create(file));
			LOG.info("Done loading " + o);
			return o;
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void save(File f, OWLOntology o, OWLOntologyFormat format){
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		try {
			man.saveOntology(o, format, IRI.create(f));
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
