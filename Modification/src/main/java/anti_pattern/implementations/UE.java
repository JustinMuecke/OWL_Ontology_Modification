package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class UE implements Anti_Pattern {

    private final OWLDataFactory dataFactory;

    public UE() {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * Pattern: c1 ⊑ ∀R.c2, c1 ⊑ ∃R.c3, Disj (c2,c3)
     * @param ontology the ontology which needs to be made inconsistent
     * @return
     */
    @Override
    public Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology) {
        Optional<OWLDisjointClassesAxiom> possibleDisjointClassesInjection = findInjectableDisjointClassesAxiom(ontology);
        if(possibleDisjointClassesInjection.isPresent()) return Optional.of(List.of(possibleDisjointClassesInjection.get()));

        Optional<OWLSubClassOfAxiom> possibleSubClassOfInjection = findInjectableSubClassOfAxiom(ontology);
        if(possibleSubClassOfInjection.isPresent()) return Optional.of(List.of(possibleSubClassOfInjection.get()));

        Optional<List<OWLAxiom>> possibleCombinationWithExistenceRestrictionInjection = findInjectableCombinationOfSubClassWithExistentialRestrictionAndDisjointClassesAxiom(ontology);
        if(possibleCombinationWithExistenceRestrictionInjection.isPresent()) return possibleCombinationWithExistenceRestrictionInjection;


        return findInjectableCombinationOfSubClassWithUniversalRestrictionAndDisjointClassesAxioms(ontology);
    }

    private Optional<OWLDisjointClassesAxiom> findInjectableDisjointClassesAxiom(OWLOntology ontology){
        for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression restrictionAroundC2 = axiom.getSuperClass();
            if (restrictionAroundC2.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)) {
                OWLClassExpression c2 = ((OWLObjectAllValuesFrom) restrictionAroundC2).getFiller();

                // Find c1 ⊑ ∃R.c3 and disjoint c3, c2
                Set<OWLObjectSomeValuesFrom> possibleC3 = ontology.axioms(AxiomType.SUBCLASS_OF)
                        .filter(ax -> ax.getSubClass().equals(c1))
                        .map(OWLSubClassOfAxiom::getSuperClass)
                        .filter(superClass -> !superClass.equals(restrictionAroundC2))
                        .filter(superClass -> superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                        .map(ax -> (OWLObjectSomeValuesFrom) ax)
                        .collect(Collectors.toSet());

                if (!possibleC3.isEmpty()) {
                    for (OWLObjectSomeValuesFrom c3 : possibleC3) {
                        OWLDisjointClassesAxiom injectableAxiom = dataFactory.getOWLDisjointClassesAxiom(c2, c3.getFiller());
                        return Optional.of(injectableAxiom);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<OWLSubClassOfAxiom> findInjectableSubClassOfAxiom(OWLOntology ontology){
        for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression restrictionAroundC2 = axiom.getSuperClass();
            if (restrictionAroundC2.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)) {
                OWLClassExpression c2 = ((OWLObjectAllValuesFrom) restrictionAroundC2).getFiller();

                // Find Disj(c2, c3) and add c1 ⊑ ∀R.c3
                Set<OWLClassExpression> possibleDisjoints = ontology.axioms(AxiomType.DISJOINT_CLASSES)
                        .filter(ax -> ax.getClassExpressions().contains(c2))
                        .map(ax -> {
                            Set<OWLClassExpression> classExpressions = new HashSet<>(ax.getClassExpressions());
                            classExpressions.remove(c2);
                            return classExpressions.iterator().next();
                        }).collect(Collectors.toSet());

                for (OWLClassExpression c : possibleDisjoints) {
                    OWLObjectSomeValuesFrom restriction = dataFactory.getOWLObjectSomeValuesFrom(((OWLObjectAllValuesFrom) restrictionAroundC2).getProperty(), c);
                    OWLSubClassOfAxiom injectableAxiom = dataFactory.getOWLSubClassOfAxiom(c1, restriction);
                    return Optional.of(injectableAxiom);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * if c1 ⊑ ∀R.c2 in the ontology find c1 ⊑ ∃R.c3, Disj (c2,c3)
     * @param ontology
     * @return
     */
    private Optional<List<OWLAxiom>> findInjectableCombinationOfSubClassWithExistentialRestrictionAndDisjointClassesAxiom(OWLOntology ontology){
        Set<OWLSubClassOfAxiom> possibleSubClassOfAxiomsContainingC2 = ontology.axioms(AxiomType.SUBCLASS_OF).filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)).collect(Collectors.toSet());
        if(possibleSubClassOfAxiomsContainingC2.isEmpty()) return Optional.empty();
        OWLSubClassOfAxiom subClassOfAxiomContainingC2 = possibleSubClassOfAxiomsContainingC2.iterator().next();
        OWLClassExpression c1 = subClassOfAxiomContainingC2.getSubClass();
        OWLClassExpression c2 = ((OWLObjectAllValuesFrom) subClassOfAxiomContainingC2.getSuperClass()).getFiller();
        OWLObjectPropertyExpression r = ((OWLObjectAllValuesFrom) subClassOfAxiomContainingC2.getSuperClass()).getProperty();
        Optional<OWLClassExpression> possibleC3 = ontology.nestedClassExpressions().filter(expression -> !expression.equals(c1) && !expression.equals(c2)).findFirst();
        if(possibleC3.isEmpty()) return Optional.empty();
        OWLClassExpression c3 = possibleC3.get();
        return Optional.of(List.of(dataFactory.getOWLSubClassOfAxiom(c1, dataFactory.getOWLObjectSomeValuesFrom(r, c3)), dataFactory.getOWLDisjointClassesAxiom(c2, c3)));
    }

    /**
     * if c1 ⊑ ∃R.c3 in the ontology find  c1 ⊑ ∀R.c2 , Disj (c2,c3)
     * @param ontology
     * @return
     */
    private Optional<List<OWLAxiom>> findInjectableCombinationOfSubClassWithUniversalRestrictionAndDisjointClassesAxioms(OWLOntology ontology){
        Set<OWLSubClassOfAxiom> possibleSubClassOfAxiomsContainingC2 = ontology.axioms(AxiomType.SUBCLASS_OF).filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)).collect(Collectors.toSet());
        if(possibleSubClassOfAxiomsContainingC2.isEmpty()) return Optional.empty();
        OWLSubClassOfAxiom subClassOfAxiomContainingC2 = possibleSubClassOfAxiomsContainingC2.iterator().next();
        OWLClassExpression c1 = subClassOfAxiomContainingC2.getSubClass();
        OWLClassExpression c2 = ((OWLObjectAllValuesFrom) subClassOfAxiomContainingC2.getSuperClass()).getFiller();
        OWLObjectPropertyExpression r = ((OWLObjectAllValuesFrom) subClassOfAxiomContainingC2.getSuperClass()).getProperty();
        Optional<OWLClassExpression> possibleC3 = ontology.nestedClassExpressions().filter(expression -> !expression.equals(c1) && !expression.equals(c2)).findFirst();
        if(possibleC3.isEmpty()) return Optional.empty();
        OWLClassExpression c3 = possibleC3.get();
        return Optional.of(List.of(dataFactory.getOWLSubClassOfAxiom(c1, dataFactory.getOWLObjectAllValuesFrom(r, c3)), dataFactory.getOWLDisjointClassesAxiom(c2, c3)));
    }



    @Override
    public String getName() {
        return "UE";
    }
}
