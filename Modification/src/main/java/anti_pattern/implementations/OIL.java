package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.Optional;
import java.util.Random;

public class OIL implements Anti_Pattern {
    private final Random randomPicker;
    private final OWLDataFactory dataFactory;
    public OIL() {
        randomPicker = new Random();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * c1 ⊑ ∀R.c2, c1 ⊑ ∀𝑅.c3, 𝐷𝑖𝑠𝑗 (𝑐2, 𝑐3)
     * @param ontology
     * @return
     */
    @Override
    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology) {
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "OIL";
    }
}
