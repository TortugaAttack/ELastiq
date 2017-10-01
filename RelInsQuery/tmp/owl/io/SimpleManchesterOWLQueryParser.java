package owl.io;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;

import main.Main;


public class SimpleManchesterOWLQueryParser extends OWLQueryParser {

	ManchesterOWLSyntaxClassExpressionParser m_parser;
	
	public SimpleManchesterOWLQueryParser(OWLOntology ontology) {
//		OWLEntityChecker entityChecker = new ShortFormEntityChecker(
//				new BidirectionalShortFormProviderAdapter(new SimpleShortFormProvider()));
		OWLEntityChecker entityChecker = new ShortFormEntityChecker(
				new BidirectionalShortFormProviderAdapter(
						Main.getOntologyManager(), ontology.getImportsClosure(),
						new SimpleIntegerAcceptingShortFormProvider()));
		
		m_parser = new ManchesterOWLSyntaxClassExpressionParser(Main.getOntologyManager().getOWLDataFactory(),
				entityChecker);
	}
	
	@Override
	public OWLClassExpression parse(String classExpression)
			throws OWLParserException {
		return m_parser.parse(classExpression);
	}
	
	
}
