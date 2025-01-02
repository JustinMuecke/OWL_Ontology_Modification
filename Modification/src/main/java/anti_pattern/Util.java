package anti_pattern;

import org.semanticweb.owlapi.model.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Util {

    private Util() {
    }

    public static Optional<OWLSubClassOfAxiom> findPossibleInjectionBasedOnSubClassAxiomWithSomeRestriction(OWLOntology ontology, OWLClassExpression c1, OWLClassExpression c2, OWLDataFactory dataFactory) {
        Set<OWLObjectSomeValuesFrom> possibleA2_2 = ontology.axioms(AxiomType.SUBCLASS_OF)
                .filter(ax -> ax.getSubClass().equals(c2))
                .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                .map(ax -> ((OWLObjectSomeValuesFrom) ax.getSuperClass()))
                .collect(Collectors.toSet());
        for (OWLObjectSomeValuesFrom allValuesFrom : possibleA2_2) {
            OWLObjectPropertyExpression property = allValuesFrom.getProperty();
            OWLClassExpression classExpression = allValuesFrom.getFiller();
            for (OWLDisjointClassesAxiom disjointClassesAxiom : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
                Set<OWLClassExpression> disjointClasses = disjointClassesAxiom.getClassExpressions();
                if (disjointClasses.remove(classExpression)) {
                    for (OWLClassExpression c4 : disjointClasses) {
                        return Optional.of(dataFactory.getOWLSubClassOfAxiom(
                                c1,
                                dataFactory.getOWLObjectAllValuesFrom(property, c4)
                        ));
                    }
                }
            }
        }
        return Optional.empty();

    }

    public static Optional<OWLSubClassOfAxiom> findPossibleInjectionBasedOnSubClassAxiomWithAllRestriction(OWLOntology ontology, OWLClassExpression c1, OWLClassExpression c2, OWLDataFactory dataFactory) {
        Set<OWLObjectAllValuesFrom> possibleA2_2 = ontology.axioms(AxiomType.SUBCLASS_OF)
                .filter(ax -> ax.getSubClass().equals(c2))
                .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                .map(ax -> ((OWLObjectAllValuesFrom) ax))
                .collect(Collectors.toSet());
        for (OWLObjectAllValuesFrom allValuesFrom : possibleA2_2) {
            OWLObjectPropertyExpression property = allValuesFrom.getProperty();
            OWLClassExpression classExpression = allValuesFrom.getFiller();
            for (OWLDisjointClassesAxiom disjointClassesAxiom : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
                Set<OWLClassExpression> disjointClasses = disjointClassesAxiom.getClassExpressions();
                if (disjointClasses.remove(classExpression)) {
                    for (OWLClassExpression c4 : disjointClasses) {
                        return Optional.of(dataFactory.getOWLSubClassOfAxiom(
                                c1,
                                dataFactory.getOWLObjectSomeValuesFrom(property, c4)
                        ));
                    }
                }
            }
        }
        return Optional.empty();
    }
    public static Optional<OWLSubClassOfAxiom> findPossibleInjectionBasedOnSubClassAxiom(OWLOntology ontology, OWLClassExpression c1, OWLClassExpression c2, OWLDataFactory dataFactory) {
        Set<OWLObjectAllValuesFrom> possibleA2_2 = ontology.axioms(AxiomType.SUBCLASS_OF)
                .filter(ax -> ax.getSubClass().equals(c2))
                .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                .map(ax -> ((OWLObjectAllValuesFrom) ax))
                .collect(Collectors.toSet());
        for(OWLObjectAllValuesFrom allValuesFrom : possibleA2_2){
            OWLObjectPropertyExpression property = allValuesFrom.getProperty();
            OWLClassExpression classExpression = allValuesFrom.getFiller();
            for(OWLDisjointClassesAxiom disjointClassesAxiom : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)){
                Set<OWLClassExpression> disjointClasses= disjointClassesAxiom.getClassExpressions();
                if(disjointClasses.remove(classExpression)){
                    for(OWLClassExpression c4 : disjointClasses){
                        return Optional.of((dataFactory.getOWLSubClassOfAxiom(
                                c1,
                                dataFactory.getOWLObjectAllValuesFrom(property, c4)
                        )));
                    }
                }
            }
        }
        return Optional.empty();

    }

    /**
     * Given an ontology and a subclass of a Pattern c1⊑∀R.c3, returns all possible class expression c3
     * @param ontology provided Ontology
     * @param subClass1 subclass c1
     * @param subClass2 subclass c2, which cant be equal to c3
     * @return Set of possible C3s
     */
    public static Set<OWLClassExpression> findFillersOfObjectAllValuesFromAxioms(OWLOntology ontology, OWLClassExpression subClass1, OWLClassExpression subClass2){
        return ontology.axioms(AxiomType.SUBCLASS_OF)
                .filter(ax -> ax.getSubClass().equals(subClass1))
                .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                .map(ax -> ((OWLObjectAllValuesFrom) ax.getSuperClass()).getFiller())
                .filter(classExpression -> !(classExpression.equals(subClass1) && classExpression.equals(subClass2)))
                .collect(Collectors.toSet());
    }
    /**
     * Given an ontology and a subclass of a Pattern c1⊑∃R.c3, returns all possible class expression c3
     * @param ontology provided Ontology
     * @param subClass1 subclass c1
     * @param subClass2 subclass c2, which cant be equal to c3
     * @return Set of possible C3s
     */
    public static Set<OWLClassExpression> findFillersOfObjectSomeValuesFromAxioms(OWLOntology ontology, OWLClassExpression subClass1, OWLClassExpression subClass2){
        return ontology.axioms(AxiomType.SUBCLASS_OF)
                .filter(ax -> ax.getSubClass().equals(subClass1))
                .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                .map(ax -> ((OWLObjectSomeValuesFrom) ax.getSuperClass()).getFiller())
                .filter(classExpression -> !(classExpression.equals(subClass1) && classExpression.equals(subClass2)))
                .collect(Collectors.toSet());

    }
}