package anti_pattern;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public interface Anti_Pattern {

    Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology);
    String getName();
}
