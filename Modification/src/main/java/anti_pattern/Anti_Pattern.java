package anti_pattern;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Optional;
import java.util.Random;

public interface Anti_Pattern {

    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology);
    public String getName();
}
