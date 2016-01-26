package owl.io;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

public class SimpleIntegerAcceptingShortFormProvider extends SimpleShortFormProvider{

	private static final long serialVersionUID = 5748394804953470793L;

	@Override
	public String getShortForm(OWLEntity entity) {
		String sf = super.getShortForm(entity);
		// in case entity iri consists of only an integer, the the SimpleShortFormProvider will not strip <>
		sf = sf.replace("<", "");
		sf = sf.replace(">", "");
		return sf;
	}
}