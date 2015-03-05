package owl.transform.el;

import org.semanticweb.owlapi.model.OWLOntology;

import owl.transform.OWLOntologyTransformer;

public class OWLToELTransformer implements OWLOntologyTransformer{

	@Override
	public void transform(OWLOntology o) {
		
		OWLToELVisitor visitor = new OWLToELVisitor();
		o.accept(visitor);
		System.out.println(visitor.getRemovedCEs() + " (nested) class expressions removed");
		System.out.println(visitor.getRemovedAxioms() + " complete axioms removed");
	}

}
