package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class OIL implements Anti_Pattern {
    private final OWLDataFactory dataFactory;
    public OIL() {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * c1 ⊑ ∀R.c2, c1 ⊑ ∀R.c3, Disj (c2, c3)
     * @param ontology ontology on which to perform search
     * @return the Axiom which can be injected into the ontology if found, else it Optional.empty
     */
    @Override
    public Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology) {
        Optional<OWLDisjointClassesAxiom> injectableDisjointClassesAxiom = findInjectableDisjointClassesAxiom(ontology);
        if(injectableDisjointClassesAxiom.isPresent()) return Optional.of(List.of(injectableDisjointClassesAxiom.get()));

        Optional<OWLSubClassOfAxiom> injectableSubClassOfAxiom = findInjectableSubClassOfAxiom(ontology);
        if(injectableSubClassOfAxiom.isPresent()) return Optional.of(List.of(injectableSubClassOfAxiom.get()));

        return findInjectableCombinationOfSubClassAndDisjointClassesAxioms(ontology);
    }

    private Optional<OWLDisjointClassesAxiom> findInjectableDisjointClassesAxiom(OWLOntology ontology){
        for(OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression restrictionAroundC2 = axiom.getSuperClass();
            if (restrictionAroundC2.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)) {
                OWLClassExpression c2 = ((OWLObjectAllValuesFrom) restrictionAroundC2).getFiller();

                //Find  c1 ⊑ ∀R.c2, c1 ⊑ ∀R.c3 and disjoin c3,c2
                Set<OWLObjectAllValuesFrom> possibleC3 = ontology.axioms(AxiomType.SUBCLASS_OF)
                        .filter(ax -> ax.getSubClass().equals(c1))
                        .map(OWLSubClassOfAxiom::getSuperClass)
                        .filter(superClass -> !superClass.equals(restrictionAroundC2))
                        .filter(superClass -> superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                        .map(ax -> (OWLObjectAllValuesFrom) ax)
                        .collect(Collectors.toSet());
                if (!possibleC3.isEmpty()) {
                    for (OWLObjectAllValuesFrom c3 : possibleC3) {
                        OWLDisjointClassesAxiom injectableAxiom = dataFactory.getOWLDisjointClassesAxiom(c2, c3.getFiller());
                        return Optional.of(injectableAxiom);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<OWLSubClassOfAxiom> findInjectableSubClassOfAxiom(OWLOntology ontology){
        for(OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression restrictionAroundC2 = axiom.getSuperClass();
            if(restrictionAroundC2.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)){
                OWLClassExpression c2 = ((OWLObjectAllValuesFrom) restrictionAroundC2).getFiller();
                Set<OWLClassExpression> possibleDisjoints = ontology.axioms(AxiomType.DISJOINT_CLASSES)
                        .filter(ax-> ax.getClassExpressions().contains(c2))
                        .map(ax -> {
                            Set<OWLClassExpression> classExpressions =ax.getClassExpressions();
                            classExpressions.remove(c2);
                            return classExpressions.iterator().next();
                        }).collect(Collectors.toSet());
                for(OWLClassExpression c : possibleDisjoints){
                    OWLObjectAllValuesFrom restriction = dataFactory.getOWLObjectAllValuesFrom(((OWLObjectAllValuesFrom) restrictionAroundC2).getProperty(), c);
                    OWLSubClassOfAxiom injectableAxiom = dataFactory.getOWLSubClassOfAxiom(c1, restriction);
                    return Optional.of(injectableAxiom);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Find the axiom c1 ⊑ ∀R.c2 and return the axioms c1 ⊑ ∀R.c3, Disj (c2, c3)
     * @param ontology the ontology in which the axioms should be injected
     * @return List of the needed axioms in case its possible
     */
    private Optional<List<OWLAxiom>> findInjectableCombinationOfSubClassAndDisjointClassesAxioms(OWLOntology ontology){
        Set<OWLSubClassOfAxiom> possibleSubClassOfAxiomsContainingC2 = ontology.axioms(AxiomType.SUBCLASS_OF).filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)).collect(Collectors.toSet());
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
        return "OIL";
    }
}
