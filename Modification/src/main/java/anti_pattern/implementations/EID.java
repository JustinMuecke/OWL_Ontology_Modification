package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

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
        Optional<OWLDisjointClassesAxiom> possibleDisjointClassesAxiom = findInjectableDisjointClassesAxioms(ontology);
        if(possibleDisjointClassesAxiom.isPresent()) return Optional.of(List.of(possibleDisjointClassesAxiom.get()));
        Optional<OWLEquivalentClassesAxiom> possibleEquivalentClassesAxiom = findInjectableEquivalentClassesAxiom(ontology);
        if(possibleEquivalentClassesAxiom.isPresent()) return Optional.of(List.of(possibleEquivalentClassesAxiom.get()));
        return findInjectableCombinationOfDisjointAndEquivalentClassAxioms(ontology);
    }

    private Optional<OWLDisjointClassesAxiom> findInjectableDisjointClassesAxioms(OWLOntology ontology){
        List<OWLEquivalentClassesAxiom> equivalentClassesAxiomList = ontology.getAxioms(AxiomType.EQUIVALENT_CLASSES).stream().toList();
        if(equivalentClassesAxiomList.isEmpty()) return Optional.empty();
        OWLEquivalentClassesAxiom equivalentClassesAxiom = equivalentClassesAxiomList.get(0);
        return Optional.of(dataFactory.getOWLDisjointClassesAxiom(equivalentClassesAxiom.getClassExpressions()));
    }

    private Optional<OWLEquivalentClassesAxiom> findInjectableEquivalentClassesAxiom(OWLOntology ontology){
        List<OWLDisjointClassesAxiom> disjointClassesAxiomList = ontology.getAxioms(AxiomType.DISJOINT_CLASSES).stream().toList();
        if(disjointClassesAxiomList.isEmpty()) return Optional.empty();
        OWLDisjointClassesAxiom disjointClassesAxiom = disjointClassesAxiomList.get(0);
        return Optional.of(dataFactory.getOWLEquivalentClassesAxiom(disjointClassesAxiom.getClassExpressions()));
    }

    private Optional<List<OWLAxiom>> findInjectableCombinationOfDisjointAndEquivalentClassAxioms(OWLOntology ontology){
        Set<OWLClassExpression> classExpressionsInOntology = ontology.getNestedClassExpressions();
        if(classExpressionsInOntology.size() < 2) return Optional.empty();
        OWLClass thing = dataFactory.getOWLThing();
        Optional<OWLClassExpression> c1 = classExpressionsInOntology.stream().filter(ax -> !ax.equals(thing)).findFirst();
        Optional<OWLClassExpression> c2 = classExpressionsInOntology.stream().filter(ax -> !ax.equals(thing)).findFirst();
        if(c1.isEmpty() || c2.isEmpty()) return Optional.empty();
        return Optional.of(List.of(dataFactory.getOWLDisjointClassesAxiom(c1.get(), c2.get()), dataFactory.getOWLEquivalentClassesAxiom(c1.get(), c2.get())));
    }
    @Override
    public String getName() {
        return "EID";
    }
}
