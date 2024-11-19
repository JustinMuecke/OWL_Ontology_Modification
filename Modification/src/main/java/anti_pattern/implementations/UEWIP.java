package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class UEWIP implements Anti_Pattern {
    private final Random randomPicker;
    private final OWLDataFactory dataFactory;

    public UEWIP() {
        randomPicker = new Random();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * c2 ⊑ ∃R1.c1, c1 ⊑ ∀R2.c3, R1 = R2^-1 Disj (c2, c3)
     * @param ontology
     * @return
     */
    @Override
    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology) {
        List<OWLAxiom> possibleInjections = new ArrayList<>();

        possibleInjections.addAll(findInjectableInversePropertyAxioms(ontology));
        possibleInjections.addAll(findInjectableSubClassOfAxioms(ontology));
        possibleInjections.addAll(findInjectableDisjointClassesAxioms(ontology));

        return possibleInjections.isEmpty() ? Optional.empty() : Optional.of(possibleInjections.get(randomPicker.nextInt(possibleInjections.size())));
    }

    /**
     * c2 ⊑ ∃R1.c1, c1 ⊑ ∀R2.c3 Disj(c2,c3) -> R1=R2^-1
     */
    private Set<OWLInverseObjectPropertiesAxiom> findInjectableInversePropertyAxioms(OWLOntology ontology){
        Set<OWLInverseObjectPropertiesAxiom> injectableAxioms = new HashSet<>();

        // Find c1 candiates
        for(OWLDisjointClassesAxiom axiom : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)){
            Set<OWLClassExpression> classExpressions = axiom.getClassExpressions();
            OWLClassExpression c1 =null;
            OWLObjectPropertyExpression r1 = null, r2 = null;
            Set<OWLSubClassOfAxiom> existsRestriction = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                    .collect(Collectors.toSet());
            Set<OWLSubClassOfAxiom> forAllRestriction = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(ax-> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                    .collect(Collectors.toSet());

            for(OWLSubClassOfAxiom subClassOfAxiom : existsRestriction) {
                OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
                if (superClass instanceof OWLObjectSomeValuesFrom someValuesFrom && classExpressions.contains(subClassOfAxiom.getSubClass())) {
                    c1 = someValuesFrom.getFiller();
                    r1 = someValuesFrom.getProperty();
                }
            }
            if(c1 == null) continue;
            for(OWLSubClassOfAxiom subClassForAll : forAllRestriction){
                OWLClassExpression superForAll = subClassForAll.getSuperClass();
                if(superForAll instanceof OWLObjectAllValuesFrom allValuesFrom
                        && classExpressions.contains(allValuesFrom.getFiller())
                        && subClassForAll.getSubClass().equals(c1)){
                    r2 = allValuesFrom.getProperty();
                    injectableAxioms.add(dataFactory.getOWLInverseObjectPropertiesAxiom(r1, r2));
                }
            }
        }
        return injectableAxioms;
    }

    /**
     * c2 ⊑ ∃R1.c1, R1 = R2^-1 Disj (c2,c3) -> c1 ⊑ ∀R2.c3,
     * @return
     */
    private Set<OWLSubClassOfAxiom> findInjectableSubClassOfAxioms(OWLOntology ontology){
        Set<OWLSubClassOfAxiom> injectableAxioms = new HashSet<>();
        for(OWLInverseObjectPropertiesAxiom inverseAxiom : ontology.getAxioms(AxiomType.INVERSE_OBJECT_PROPERTIES)){
            OWLObjectPropertyExpression r1 = inverseAxiom.getFirstProperty();
            OWLObjectPropertyExpression r2 = inverseAxiom.getSecondProperty();
            for(OWLSubClassOfAxiom subClassOfAxiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)){
                OWLClassExpression c1 = null, c2 =null;
                if(subClassOfAxiom.getSuperClass() instanceof OWLObjectSomeValuesFrom someValuesFrom && someValuesFrom.getProperty().equals(r1)){
                    c2 = subClassOfAxiom.getSubClass();
                    c1 = someValuesFrom.getFiller();
                }
                if(c1 == null || c2 == null) continue;
                for(OWLDisjointClassesAxiom disjointClassesAxiom : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)){
                    Set<OWLClassExpression> disjointClasses = disjointClassesAxiom.getClassExpressions();
                    if(disjointClasses.contains(c2)){
                        disjointClasses.remove(c2);
                        for(OWLClassExpression c3 : disjointClasses){
                            injectableAxioms.add(
                                    dataFactory.getOWLSubClassOfAxiom(
                                            c1,
                                            dataFactory.getOWLObjectAllValuesFrom(r2,c3)
                                    )
                            );
                        }
                    }
                }
            }
        }
        return injectableAxioms;
    }

    /**
     * c2 ⊑ ∃R1.c1, c1 ⊑ ∀R2.c3, R1 = R2^-1 -> Disj (c2, c3)
     * @param ontology
     * @return
     */
    private Set<OWLDisjointClassesAxiom> findInjectableDisjointClassesAxioms(OWLOntology ontology){
        Set<OWLDisjointClassesAxiom> injectableAxioms = new HashSet<>();
        for(OWLInverseObjectPropertiesAxiom inverseAxiom : ontology.getAxioms(AxiomType.INVERSE_OBJECT_PROPERTIES)) {
            OWLObjectPropertyExpression r1 = inverseAxiom.getFirstProperty();
            OWLObjectPropertyExpression r2 = inverseAxiom.getSecondProperty();
            Set<OWLSubClassOfAxiom> existsRestriction = ontology.axioms(AxiomType.SUBCLASS_OF).filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)).collect(Collectors.toSet());
            Set<OWLSubClassOfAxiom> forAllRestriction = ontology.axioms(AxiomType.SUBCLASS_OF).filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)).collect(Collectors.toSet());
            OWLClassExpression c1 = null, c2 = null, c3 = null;

            for (OWLSubClassOfAxiom subClassOfAxiom : existsRestriction) {
                OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
                if (superClass instanceof OWLObjectSomeValuesFrom someValuesFrom
                        && someValuesFrom.getProperty().equals(r1)) {
                    c1 = someValuesFrom.getFiller();
                    c2 = subClassOfAxiom.getSubClass();
                }
            }
            if (c1 == null) continue;
            for (OWLSubClassOfAxiom forAllSubClass : forAllRestriction){
                OWLClassExpression superClass = forAllSubClass.getSuperClass();
                if(superClass instanceof OWLObjectAllValuesFrom allValuesFrom
                        && forAllSubClass.getSubClass().equals(c1)
                        && allValuesFrom.getProperty().equals(r2)){
                    c3 = allValuesFrom.getFiller();
                }
            }
            if(c2 != null && c3 != null && !c2.equals(c3)){
                injectableAxioms.add(dataFactory.getOWLDisjointClassesAxiom(c2,c3));
            }
        }
        return injectableAxioms;
    }

    @Override
    public String getName() {
        return "UEWIP";
    }
}
