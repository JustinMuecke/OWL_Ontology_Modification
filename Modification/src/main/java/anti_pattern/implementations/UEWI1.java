package anti_pattern.implementations;
import anti_pattern.Util;
import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UEWI1 implements Anti_Pattern {
    private final Random randomPicker;
    private final OWLDataFactory dataFactory;

    public UEWI1() {
        randomPicker = new Random();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * c1⊑c2, c1 ⊑ ∃R.c3, c2 ⊑ ∀R.c4, Disj (c3, c4)
     * @param ontology
     * @return
     */
    @Override
    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology) {
        List<OWLAxiom> possibleInjections = new ArrayList<>();
        //a1: c1⊑c2, a2: c1⊑∀R.c3, a3: c2⊑∀R.c4 in ontology -> insert Disj(c3,c4)

        for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression c2 = axiom.getSuperClass();
            if (c1.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)
                    && c2.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {

                Set<OWLClassExpression> possibleC3 = Util.findFillersOfObjectSomeValuesFromAxioms(ontology, c1, c2);
                Set<OWLClassExpression> possibleC4 = Util.findFillersOfObjectAllValuesFromAxioms(ontology, c2, c1);

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
        //a1: c1⊑c2, a2.1: c1⊑∀R.c3, a3: Disj(c3,c4) in ontology -> insert c2⊑∃R.c4
        //           a2.2: c2⊑∃R.c4                              -> insert c1⊑∀R.c3
        for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression c2 = axiom.getSuperClass();
            if (c1.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)
                    && c2.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
                //a2.1
                Util.findPossibleInjectionBasedOnSubClassAxiomWithSomeRestriction(ontology,possibleInjections,c1, c2, dataFactory);
                //a2.2
                Util.findPossibleInjectionBasedOnSubClassAxiomWithAllRestriction(ontology, possibleInjections, c2, c1,dataFactory);
            }
        }
        // c1 ⊑ ∃R.c3, c2 ⊑ ∀R.c4, Disj (c3, c4) in ontology -> insert c1⊑c2
        for (OWLDisjointClassesAxiom axiom : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
            Set<OWLClassExpression> classes = axiom.getClassExpressions();
            Stream<OWLSubClassOfAxiom> axiomStream = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(subClassOfAxiom -> subClassOfAxiom.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM));
            Set<OWLSubClassOfAxiom> possibleC1SubClassAxioms = axiomStream
                    .filter(ax -> classes.contains(((OWLObjectSomeValuesFrom) ax.getSuperClass()).getFiller()))
                    .collect(Collectors.toSet());
            for (OWLSubClassOfAxiom possibleSubClassAxiom : possibleC1SubClassAxioms) {
                OWLObjectPropertyExpression property = ((OWLObjectAllValuesFrom) possibleSubClassAxiom.getSuperClass()).getProperty();
                OWLClassExpression c3 = possibleSubClassAxiom.getSubClass();

                Set<OWLSubClassOfAxiom> foundPattern = ontology
                        //c2 ⊑ x
                        .axioms(AxiomType.SUBCLASS_OF)
                        //x=∀R.c4
                        .filter(subClassOfAxiom-> subClassOfAxiom.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                        // c4 in Disj(x..)
                        .filter(ax -> classes.contains(((OWLObjectAllValuesFrom) ax.getSuperClass()).getFiller()))
                        //R is same as c1 ⊑ ∃R.c3
                        .filter(ax -> ((OWLObjectAllValuesFrom) ax.getSuperClass()).getProperty().equals(property))
                        //c4 != c1 in c1 ⊑ ∃R.c3
                        .filter(ax -> !ax.getSubClass().equals(c3))
                        //c4 != c3 in c1 ⊑ ∃R.c3
                        .filter(ax -> !((OWLObjectSomeValuesFrom) ax.getSuperClass()).getFiller().equals(((OWLObjectSomeValuesFrom) possibleSubClassAxiom).getFiller()))
                        //x=c4
                        .map(ax -> ((OWLObjectAllValuesFrom) ax.getSuperClass()).getFiller())
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
