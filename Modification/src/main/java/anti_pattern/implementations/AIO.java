package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class AIO implements Anti_Pattern {

    private final OWLDataFactory dataFactory;

    public AIO() {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }
    /**
     *  Looks to complete the Pattern: Disj(c2, c3), c1 ⊑ ∃R.c2, c1 ⊑ ∃R.(c2 ⊓ c3)
     *  @param ontology the ontology in which the missing axiom(s) should be injected
     *  @return list of all axioms needed to be injected to make the ontology inconsistent
     */

    @Override
    public Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology) {

        Optional<OWLSubClassOfAxiom> injectableSubClassOfAxiom = findInjectableSubClassOfAxiom(ontology);
        if(injectableSubClassOfAxiom.isPresent()) return Optional.of(List.of(injectableSubClassOfAxiom.get()));

        Optional<OWLDisjointClassesAxiom> injectableDisjointClassesAxiom = findInjectableDisjointClassesAxiom(ontology);
        if(injectableDisjointClassesAxiom.isPresent()) return Optional.of(List.of(injectableDisjointClassesAxiom.get()));

        return findInjectableDisjointClassesAndSUbClassOfAxiom(ontology);
    }

    /**
     * Checks whether the axioms Disj(c2, c3) and c1 ⊑ ∃R.c2 are in the ontology and if yes finds the axiom c1 ⊑ ∃R.(c2 ⊓ c3)
     * @param ontology the ontology we want to check for axiom injection
     * @return axiom of the form c1 ⊑ ∃R.(c2 ⊓ c3) if applicable
     */
    private Optional<OWLSubClassOfAxiom> findInjectableSubClassOfAxiom(OWLOntology ontology){
        // If Disj(c2, c3) is in the ontology and c1 ⊑ ∃R.c2, add c1 ⊑ ∃R.(c2 ⊓ c3)
        Set<OWLDisjointClassesAxiom> disjointClassesAxiomSet = ontology.getAxioms(AxiomType.DISJOINT_CLASSES);
        for(OWLDisjointClassesAxiom disjointClassesAxiom : disjointClassesAxiomSet){


            List<OWLClassExpression> disjointClasses = disjointClassesAxiom.getClassExpressionsAsList();

            if (disjointClasses.size() < 2) continue;  // Skip if there aren't at least two disjoint classes
            OWLClassExpression c2 = disjointClasses.get(0);
            OWLClassExpression c3 = disjointClasses.get(1);

            Optional<OWLSubClassOfAxiom> injectionAxiom =ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(axiom -> {
                        // Check if it has an existential restriction of the form c1 ⊑ ∃R.c2
                        if (axiom.getSubClass().getClassExpressionType().equals(ClassExpressionType.OWL_CLASS) &&
                                axiom.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
                            OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) axiom.getSuperClass();
                            return someValuesFrom.getFiller().equals(c2);  // Check if filler is c2
                        }
                        return false;
                    })
                    .findFirst();
            if(injectionAxiom.isEmpty()) continue;

            OWLClassExpression c1 = injectionAxiom.get().getSubClass();
            OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) injectionAxiom.get().getSuperClass();
            OWLObjectPropertyExpression property = someValuesFrom.getProperty();
            OWLObjectIntersectionOf intersection = dataFactory.getOWLObjectIntersectionOf(c2, c3);
            OWLObjectSomeValuesFrom newSomeValuesFrom = dataFactory.getOWLObjectSomeValuesFrom(property, intersection);
            OWLSubClassOfAxiom newAxiom = dataFactory.getOWLSubClassOfAxiom(c1, newSomeValuesFrom);
            return Optional.of(newAxiom);
        }
        return Optional.empty();
    }

    /**
     * If the Axioms c1 ⊑ ∃R.c2, c1 ⊑ ∃R.(c2 ⊓ c3) are in the ontology, it finds the disjoint Classes Axiom Disj(c2,c3)
     * @param ontology the Ontology which we want to inject the Axiom into
     * @return the Disjoint Classes Axiom if it applicable
     */
    private Optional<OWLDisjointClassesAxiom> findInjectableDisjointClassesAxiom(OWLOntology ontology) {
        Set<OWLSubClassOfAxiom> subClassOfAxiomSet = ontology.getAxioms(AxiomType.SUBCLASS_OF);
        for(OWLSubClassOfAxiom subClassOfAxiom : subClassOfAxiomSet){
            OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
            if(!superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) continue;
            OWLObjectSomeValuesFrom restriction = (OWLObjectSomeValuesFrom) superClass;
            if(!restriction.getFiller().getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF)) continue;
            OWLObjectIntersectionOf filler = (OWLObjectIntersectionOf) restriction.getFiller();
            Set<OWLClassExpression> expressionsToBeDisjointed = filler.getOperands();
            OWLDisjointClassesAxiom injectionAxiom = dataFactory.getOWLDisjointClassesAxiom(expressionsToBeDisjointed);
            return Optional.of(injectionAxiom);
        }
        return Optional.empty();
    }

    /**
     * If c1 ⊑ ∃R.c2 is in the Ontology, return the axioms Disj(c2, c3) and c1 ⊑ ∃R.(c2 ⊓ c3)
     * @param ontology which needs to be made inconsistent
     * @return List of OWLAxioms which need to be injected to make the ontology inconsistent
     */
    private Optional<List<OWLAxiom>> findInjectableDisjointClassesAndSUbClassOfAxiom(OWLOntology ontology){
        Set<OWLSubClassOfAxiom> subClassOfAxiomSet = ontology.axioms(AxiomType.SUBCLASS_OF).filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)).collect(Collectors.toSet());
        if(subClassOfAxiomSet.isEmpty()) return Optional.empty();
        OWLSubClassOfAxiom subClassOfAxiom = subClassOfAxiomSet.iterator().next();
        OWLClassExpression c1 = subClassOfAxiom.getSubClass();
        OWLObjectPropertyExpression r = ((OWLObjectSomeValuesFrom) subClassOfAxiom.getSuperClass()).getProperty();
        OWLClassExpression c2 = ((OWLObjectSomeValuesFrom) subClassOfAxiom.getSuperClass()).getFiller();
        Optional<OWLClassExpression> possibleC3 = ontology.nestedClassExpressions().filter(classExpression -> !classExpression.equals(c1) && ! classExpression.equals(c2)).findFirst();
        if(possibleC3.isEmpty()) return Optional.empty();
        OWLClassExpression c3 = possibleC3.get();
        return Optional.of(List.of(dataFactory.getOWLSubClassOfAxiom(c1, dataFactory.getOWLObjectSomeValuesFrom(r, dataFactory.getOWLObjectIntersectionOf(c2,c3))), dataFactory.getOWLDisjointClassesAxiom(c2, c3)));
    }

    @Override
    public String getName() {
        return "AIO";
    }
}
