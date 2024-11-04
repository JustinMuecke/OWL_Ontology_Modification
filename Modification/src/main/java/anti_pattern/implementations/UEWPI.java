package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.Optional;
import java.util.Random;

public class UEWPI implements Anti_Pattern {
    private final Random randomPicker;
    private final OWLDataFactory dataFactory;

    public UEWPI(Random randomPicker, OWLDataFactory dataFactory) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        this.randomPicker = randomPicker;
        this.dataFactory = dataFactory;
    }

    /**
     * 𝑅1 ⊑ 𝑅2, 𝑐1 ⊑ ∃𝑅1.𝑐2, 𝑐1 ⊑ ∀𝑅2.𝑐3, 𝐷𝑖𝑠𝑗(𝑐2, 𝑐3)
     * @param ontology
     * @return
     */
    @Override
    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology) {
        // 𝑅1 ⊑ 𝑅2, 𝑐1 ⊑ ∃𝑅1.𝑐2, 𝑐1 ⊑ ∀𝑅2.𝑐3 in ontology -> insert 𝐷𝑖𝑠𝑗(𝑐2, 𝑐3)

        // 𝑅1 ⊑ 𝑅2, 𝑐1 ⊑ ∃𝑅1.𝑐2, 𝐷𝑖𝑠𝑗(𝑐2, 𝑐3) in ontology -> insert 𝑐1 ⊑ ∀𝑅2.𝑐3

        // 𝑅1 ⊑ 𝑅2, 𝑐1 ⊑ ∀𝑅2.𝑐3, 𝐷𝑖𝑠𝑗(𝑐2, 𝑐3) in ontology -> insert 𝑐1 ⊑ ∃𝑅1.𝑐2

        // 𝑐1 ⊑ ∃𝑅1.𝑐2, 𝑐1 ⊑ ∀𝑅2.𝑐3, 𝐷𝑖𝑠𝑗(𝑐2, 𝑐3) in ontology -> insert 𝑅1 ⊑ 𝑅2


        return Optional.empty();
    }

    @Override
    public String getName() {
        return "";
    }
}
