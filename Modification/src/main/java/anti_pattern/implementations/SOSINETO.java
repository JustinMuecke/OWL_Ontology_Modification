package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class SOSINETO implements Anti_Pattern {

    private final Random randomPicker;
    private final OWLDataFactory dataFactory;

    public SOSINETO() {
        randomPicker = new Random();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * c1 ⊑ ∃R.c2, c1 ⊑ ∃R.c3, c1 ⊑ ≤1.T , Disj (c2, c3)
     *
     * @param ontology
     * @return
     */
    @Override
    public Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology) {
        Optional<OWLSubClassOfAxiom> result = findInjectableSubClassOfCardinalityRestraint(ontology);
        if(result.isPresent()) return Optional.of(List.of(result.get()));
        return Optional.empty();
    }


    private Optional<List<OWLAxiom>> findInjectableCombinationOfCardinalityRestraintDisjointClassesAndSubClassAxioms(OWLOntology ontology){
        Set<OWLSubClassOfAxiom> possibleSubClassOfAxiomsContainingC2 = ontology.axioms(AxiomType.SUBCLASS_OF).filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)).collect(Collectors.toSet());
        if(possibleSubClassOfAxiomsContainingC2.isEmpty()) return Optional.empty();
        OWLSubClassOfAxiom subClassOfAxiomContainingC2 = possibleSubClassOfAxiomsContainingC2.iterator().next();
        OWLClassExpression c1 = subClassOfAxiomContainingC2.getSubClass();
        OWLClassExpression c2 = ((OWLObjectAllValuesFrom) subClassOfAxiomContainingC2.getSuperClass()).getFiller();
        OWLObjectPropertyExpression r = ((OWLObjectAllValuesFrom) subClassOfAxiomContainingC2.getSuperClass()).getProperty();
        Optional<OWLClassExpression> possibleC3 = ontology.nestedClassExpressions().filter(expression -> !expression.equals(c1) && !expression.equals(c2)).findFirst();
        if(possibleC3.isEmpty()) return Optional.empty();
        OWLClassExpression c3 = possibleC3.get();
        return Optional.of(List.of(dataFactory.getOWLSubClassOfAxiom(c1, dataFactory.getOWLObjectSomeValuesFrom(r, c3)), dataFactory.getOWLSubClassOfAxiom(c1, dataFactory.getOWLObjectMaxCardinality(1, r)), dataFactory.getOWLDisjointClassesAxiom(c2, c3)));

    }

    /**
     * c1 ⊑ ∃R.c2, c1 ⊑ ∃R.c3, Disj (c2, c3) -> c1 ⊑ ≤1.T ,
     *
     * @param ontology
     * @return
     */
    private Optional<OWLSubClassOfAxiom> findInjectableSubClassOfCardinalityRestraint(OWLOntology ontology) {
        Set<OWLSubClassOfAxiom> injectableAxioms = new HashSet<>();
        Set<OWLClassExpression> c1Candidates = ontology.axioms(AxiomType.SUBCLASS_OF)
                .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                .map(OWLSubClassOfAxiom::getSubClass)
                .collect(Collectors.toSet());

        for (OWLClassExpression c1 : c1Candidates) {
            Set<OWLObjectPropertyExpression> rCandidates = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                    .filter(ax -> ax.getSubClass().equals(c1))
                    .map(ax -> (OWLObjectSomeValuesFrom) ax.getSuperClass())
                    .map(OWLObjectRestriction::getProperty)
                    .collect(Collectors.toSet());

            for (OWLObjectPropertyExpression r : rCandidates) {
                Set<OWLClassExpression> connections = ontology.axioms(AxiomType.SUBCLASS_OF)
                        .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                        .filter(ax -> ax.getSubClass().equals(c1))
                        .map(ax -> (OWLObjectSomeValuesFrom) ax.getSuperClass())
                        .filter(ax -> ax.getProperty().equals(r))
                        .map(HasFiller::getFiller)
                        .collect(Collectors.toSet());

                if (connections.size() > 1) {
                    for (OWLDisjointClassesAxiom disjointClassesAxiom : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
                        Set<OWLClassExpression> disjointClasses = disjointClassesAxiom.getClassExpressions();

                        Set<OWLClassExpression> intersection = new HashSet<>(disjointClasses);
                        intersection.retainAll(connections);

                        if (intersection.size() > 1) {
                            // Found disjoint classes among connections, add max cardinality axiom
                            OWLSubClassOfAxiom maxCardinalityAxiom = dataFactory.getOWLSubClassOfAxiom(
                                    c1,
                                    dataFactory.getOWLObjectMaxCardinality(1, r)
                            );
                            return Optional.of(maxCardinalityAxiom);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }


    @Override
    public String getName() {
        return "SOSINETO";
    }
}
