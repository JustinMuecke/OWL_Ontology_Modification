package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
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
    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology) {
        return Optional.empty();
    }

    /**
     * c1 ⊑ ∃R.c2, c1 ⊑ ∃R.c3, Disj (c2, c3) -> c1 ⊑ ≤1.T ,
     *
     * @param ontology
     * @return
     */
    private Set<OWLSubClassOfAxiom> findInjectableSubClassOfCardinalityRestraint(OWLOntology ontology) {
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
                            injectableAxioms.add(maxCardinalityAxiom);
                            break;  // Only need one such axiom per `c1` and `r`
                        }
                    }
                }
            }
        }
        return injectableAxioms;
    }


    @Override
    public String getName() {
        return "";
    }
}
