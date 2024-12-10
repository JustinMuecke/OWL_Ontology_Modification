package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class EID implements Anti_Pattern {

    private final Random randomPicker;
    private final OWLDataFactory dataFactory;
    public EID() {
        randomPicker = new Random();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * EquivalenceIsDifference: two classes are equivalent and disjoint in the same ontology.
     * @param ontology Ontology which we want to make inconsistent
     * @return Either the axiom which makes the ontology inconsistent if injected or an empty optional
     */
    @Override
    public Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology) {
        List<OWLEquivalentClassesAxiom> equivalentClassesAxiomList = ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES).stream().toList();
        List<OWLDisjointClassesAxiom> disjointClassesAxiomList = ontology.getAxioms(AxiomType.DISJOINT_CLASSES).stream().toList();
        if(equivalentClassesAxiomList.isEmpty() && disjointClassesAxiomList.isEmpty()) return Optional.empty();

        // Pick one Axiom and Return the associated inverse
        int chooseBetweenEquivalentAndDisjointAxioms = this.randomPicker.nextInt(2);
        if (chooseBetweenEquivalentAndDisjointAxioms == 0){
            int randomIndex=randomPicker.nextInt(equivalentClassesAxiomList.size());
            OWLEquivalentClassesAxiom equivalentClassesAxiom = equivalentClassesAxiomList.get(randomIndex);
            OWLClassExpression[] classesInAxiom = equivalentClassesAxiom.getClassExpressions().toArray(new OWLClassExpression[0]);
            OWLDisjointClassesAxiom disjointClassesAxiom = dataFactory.getOWLDisjointClassesAxiom(classesInAxiom[0], classesInAxiom[1]);
            return Optional.of(List.of(disjointClassesAxiom));
        }
        int randomIndex=randomPicker.nextInt(disjointClassesAxiomList.size());
        OWLDisjointClassesAxiom disjointClassesAxiom = disjointClassesAxiomList.get(randomIndex);
        OWLClassExpression[] classesInAxiom = disjointClassesAxiom.getClassExpressions().toArray(new OWLClassExpression[0]);
        OWLEquivalentClassesAxiom equivalentClassesAxiom = dataFactory.getOWLEquivalentClassesAxiom(classesInAxiom[0], classesInAxiom[1]);
        return Optional.of(List.of(equivalentClassesAxiom));
    }

    @Override
    public String getName() {
        return "EID";
    }
}
