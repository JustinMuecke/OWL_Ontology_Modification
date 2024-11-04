package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.Optional;
import java.util.Random;

public class UEWIP implements Anti_Pattern {
    private final Random randomPicker;
    private final OWLDataFactory dataFactory;

    public UEWIP(Random randomPicker, OWLDataFactory dataFactory) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        this.randomPicker = randomPicker;
        this.dataFactory = dataFactory;
    }

    @Override
    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology) {
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "";
    }
}
