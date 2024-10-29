package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;

public class AIO implements Anti_Pattern {
    private final Random randomPicker;
    private final OWLDataFactory dataFactory;
    public AIO() {
        randomPicker = new Random();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }
    /**
     * Pattern : c1 ⊑ ∃R.(c2 ⊓ c3), 𝐷𝑖𝑠𝑗(c2, c3)
     */

    @Override
    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology) {
        List<OWLAxiom> possibleInjections = new LinkedList<>();
        // If 𝐷𝑖𝑠𝑗(c2, c3) is in the ontology and c1 ⊑ ∃R.c2, add c1 ⊑ ∃R.(c2 ⊓ c3)
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

                        // If such an axiom exists, create a new axiom c1 ⊑ ∃R.(c2 ⊓ c3)
            OWLClassExpression c1 = injectionAxiom.get().getSubClass();
            OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) injectionAxiom.get().getSuperClass();
            OWLObjectPropertyExpression property = someValuesFrom.getProperty();
            // Create the intersection c2 ⊓ c3
            OWLObjectIntersectionOf intersection = dataFactory.getOWLObjectIntersectionOf(c2, c3);
            // Create the new existential restriction ∃R.(c2 ⊓ c3)
            OWLObjectSomeValuesFrom newSomeValuesFrom = dataFactory.getOWLObjectSomeValuesFrom(property, intersection);
            // Create the new subclass axiom c1 ⊑ ∃R.(c2 ⊓ c3)
            OWLSubClassOfAxiom newAxiom = dataFactory.getOWLSubClassOfAxiom(c1, newSomeValuesFrom);
            possibleInjections.add(newAxiom);
        }

        // If c1 ⊑ ∃R.(c2 ⊓ c3) is in the ontology, add 𝐷𝑖𝑠𝑗(c2, c3)
        Set<OWLSubClassOfAxiom> subClassOfAxiomSet = ontology.getAxioms(AxiomType.SUBCLASS_OF);
        for(OWLSubClassOfAxiom subClassOfAxiom : subClassOfAxiomSet){
            OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
            if(!superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) continue;
            OWLObjectSomeValuesFrom restriction = (OWLObjectSomeValuesFrom) superClass;
            if(!restriction.getFiller().getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF)) continue;
            OWLObjectIntersectionOf filler = (OWLObjectIntersectionOf) restriction.getFiller();
            Set<OWLClassExpression> expressionsToBeDisjointed = filler.getOperands();
            OWLDisjointClassesAxiom injectionAxiom = dataFactory.getOWLDisjointClassesAxiom(expressionsToBeDisjointed);
            possibleInjections.add(injectionAxiom);
        }

        if(possibleInjections.isEmpty()) return Optional.empty();
        int randomIndex = randomPicker.nextInt(possibleInjections.size());
        return Optional.of(possibleInjections.get(randomIndex));
    }

    @Override
    public String getName() {
        return "AIO";
    }
}
