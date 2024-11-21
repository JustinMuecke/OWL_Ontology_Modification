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
     *
     */

    @Override
    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology) {
        List<OWLAxiom> possibleInjections = new LinkedList<>();
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

    @Override
    public String getName() {
        return "AIO";
    }
}
