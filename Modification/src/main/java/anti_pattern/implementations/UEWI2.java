package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import anti_pattern.Util;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UEWI2 implements Anti_Pattern {
    private final Random randomPicker;
    private final OWLDataFactory dataFactory;

    public UEWI2(Random randomPicker, OWLDataFactory dataFactory) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        this.randomPicker = randomPicker;
        this.dataFactory = dataFactory;
    }

    @Override
    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology) {
        List<OWLAxiom> possibleInjections = new ArrayList<>();
        //a1: 𝑐1⊑𝑐2, a2: 𝑐1⊑∀𝑅.𝑐3, a3: 𝑐2⊑∀𝑅.𝑐4 in ontology -> insert 𝐷𝑖𝑠𝑗(c3,c4)

        for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression c2 = axiom.getSuperClass();
            if (c1.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)
                    && c2.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {

                Set<OWLClassExpression> possibleC3 = Util.findFillersOfObjectSomeValuesFromAxioms(ontology, c2, c1);
                Set<OWLClassExpression> possibleC4 = Util.findFillersOfObjectAllValuesFromAxioms(ontology, c1, c2);

                if (!(possibleC3.isEmpty() && possibleC4.isEmpty())) {
                    possibleC3.forEach(
                            c3 -> {
                                possibleC4.forEach(
                                        c4 -> possibleInjections.add(dataFactory.getOWLDisjointClassesAxiom(c3, c4))
                                );
                            }
                    );
                }
                break;
            }
        }
        //a1: 𝑐1⊑𝑐2, a2.1: 𝑐1⊑∀𝑅.𝑐3, a3: 𝐷𝑖𝑠𝑗(c3,c4) in ontology -> insert 𝑐2⊑∃𝑅.𝑐4
        //           a2.2: 𝑐2⊑∃𝑅.𝑐4                              -> insert 𝑐1⊑∀𝑅.𝑐3
        for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression c2 = axiom.getSuperClass();
            if (c1.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)
                    && c2.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
                //a2.1
                Util.findPossibleInjectionBasedOnSubClassAxiomWithSomeRestriction(ontology,possibleInjections,c2, c1, dataFactory);
                //a2.2
                Util.findPossibleInjectionBasedOnSubClassAxiomWithAllRestriction(ontology, possibleInjections, c1, c2,dataFactory);
            }
        }
        // 𝑐1 ⊑ ∃𝑅.𝑐3, 𝑐2 ⊑ ∀𝑅.𝑐4, 𝐷𝑖𝑠𝑗 (𝑐3, 𝑐4) in ontology -> insert 𝑐1⊑𝑐2
        for (OWLDisjointClassesAxiom axiom : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
            Set<OWLClassExpression> classes = axiom.getClassExpressions();
            Stream<OWLSubClassOfAxiom> axiomStream = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(subClassOfAxiom -> subClassOfAxiom.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM));
            Set<OWLSubClassOfAxiom> possibleC1SubClassAxioms = axiomStream
                    .filter(ax -> classes.contains(((OWLObjectAllValuesFrom) ax.getSuperClass()).getFiller()))
                    .collect(Collectors.toSet());
            for (OWLSubClassOfAxiom possibleSubClassAxiom : possibleC1SubClassAxioms) {
                OWLObjectPropertyExpression property = ((OWLObjectSomeValuesFrom) possibleSubClassAxiom.getSuperClass()).getProperty();
                OWLClassExpression c3 = possibleSubClassAxiom.getSubClass();

                Set<OWLSubClassOfAxiom> foundPattern = ontology
                        //𝑐2 ⊑ x
                        .axioms(AxiomType.SUBCLASS_OF)
                        //x=∀𝑅.𝑐4
                        .filter(subClassOfAxiom-> subClassOfAxiom.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                        // c4 in Disj(x..)
                        .filter(ax -> classes.contains(((OWLObjectSomeValuesFrom) ax.getSuperClass()).getFiller()))
                        //R is same as 𝑐1 ⊑ ∃𝑅.𝑐3
                        .filter(ax -> ((OWLObjectSomeValuesFrom) ax.getSuperClass()).getProperty().equals(property))
                        //c4 != c1 in 𝑐1 ⊑ ∃𝑅.𝑐3
                        .filter(ax -> !ax.getSubClass().equals(c3))
                        //c4 != c3 in 𝑐1 ⊑ ∃𝑅.𝑐3
                        .filter(ax -> !((OWLObjectSomeValuesFrom) ax.getSuperClass()).getFiller().equals(((OWLObjectAllValuesFrom) possibleSubClassAxiom).getFiller()))
                        //x=c4
                        .map(ax -> ((OWLObjectSomeValuesFrom) ax.getSuperClass()).getFiller())
                        //x = Disjoin(c3,c4)
                        .map(cls -> dataFactory.getOWLSubClassOfAxiom(c3, cls))
                        .collect(Collectors.toSet());
                if (!foundPattern.isEmpty()) {
                    possibleInjections.addAll(foundPattern);
                }
            }

        }
        return possibleInjections.isEmpty() ? Optional.empty() : Optional.of(possibleInjections.get(randomPicker.nextInt(possibleInjections.size())));
    }












    @Override
    public String getName() {
        return "";
    }
}
