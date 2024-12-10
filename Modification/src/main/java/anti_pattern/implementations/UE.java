package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class UE implements Anti_Pattern {

    private final Random randomPicker;
    private final OWLDataFactory dataFactory;

    public UE() {
        randomPicker = new Random();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * Pattern: c1 ⊑ ∀R.c2, c1 ⊑ ∃R.c3, Disj (c2,c3)
     * @param ontology
     * @return
     */
    @Override
    public Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology) {
        for(OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression restrictionAroundC2 = axiom.getSuperClass();
            if(restrictionAroundC2.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)){
                OWLClassExpression c2 = ((OWLObjectAllValuesFrom) restrictionAroundC2).getFiller();

                //Find  c1 ⊑ ∀R.c2, c1 ⊑ ∃R.c3 and disjoin c3,c2
                Set<OWLObjectSomeValuesFrom> possibleC3 = ontology.axioms(AxiomType.SUBCLASS_OF)
                        .filter(ax -> ax.getSubClass().equals(c1))
                        .map(OWLSubClassOfAxiom::getSuperClass)
                        .filter(superClass -> !superClass.equals(restrictionAroundC2))
                        .filter(superClass -> superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                        .map(ax -> (OWLObjectSomeValuesFrom) ax)
                        .collect(Collectors.toSet());
                if(!possibleC3.isEmpty()){
                    for(OWLObjectSomeValuesFrom c3 : possibleC3){
                        OWLDisjointClassesAxiom injectableAxiom = dataFactory.getOWLDisjointClassesAxiom(c2, c3.getFiller());
                        return Optional.of(List.of(injectableAxiom));
                    }
                }
                //Find Disj(c2, c3) and add c1 ⊑ ∀R.c3.
                Set<OWLClassExpression> possibleDisjoints = ontology.axioms(AxiomType.DISJOINT_CLASSES)
                        .filter(ax-> ax.getClassExpressions().contains(c2))
                        .map(ax -> {
                            Set<OWLClassExpression> classExpressions =ax.getClassExpressions();
                            classExpressions.remove(c2);
                            return classExpressions.iterator().next();
                        }).collect(Collectors.toSet());
                for(OWLClassExpression c : possibleDisjoints){
                    OWLObjectSomeValuesFrom restriction = dataFactory.getOWLObjectSomeValuesFrom(((OWLObjectAllValuesFrom) restrictionAroundC2).getProperty(), c);
                    OWLSubClassOfAxiom injectableAxiom = dataFactory.getOWLSubClassOfAxiom(c1, restriction);
                    return Optional.of(List.of(injectableAxiom));
                }
            }
        }
        return Optional.empty();
    }


    @Override
    public String getName() {
        return "UE";
    }
}
