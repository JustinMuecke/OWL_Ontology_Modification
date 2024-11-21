package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class OILWPI implements Anti_Pattern {
    private final Random randomPicker;
    private final OWLDataFactory dataFactory;

    public OILWPI() {
        randomPicker = new Random();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * R1 ⊑ R2, c1 ⊑ ∀R1.c2, c1 ⊑ ∀R2.c3, Disj (c2, c3)
     * @param ontology
     * @return
     */
    @Override
    public Optional<OWLAxiom> checkForPossiblePatternCompletion(OWLOntology ontology) {
        List<OWLAxiom> possibleInjections = new ArrayList<>();

        Set<OWLSubObjectPropertyOfAxiom> subPropertyAxiomSet = ontology.getAxioms(AxiomType.SUB_OBJECT_PROPERTY);
        for(OWLSubObjectPropertyOfAxiom subPropertyAxiom : subPropertyAxiomSet) {
            OWLObjectPropertyExpression r1 = subPropertyAxiom.getSubProperty();
            OWLObjectPropertyExpression r2 = subPropertyAxiom.getSuperProperty();
            // R1 ⊑ R2, c1 ⊑ ∀R1.c2, c1 ⊑ ∀R2.c3, -> Disj(c2, c3)
            Set<OWLSubClassOfAxiom> subClassOfAxiomSet = ontology.axioms(AxiomType.SUBCLASS_OF)
                        .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                        .filter(ax -> ((OWLObjectAllValuesFrom)ax.getSuperClass()).getProperty().equals(r1)
                        || ((OWLObjectAllValuesFrom)ax.getSuperClass()).getProperty().equals(r2))
                        .collect(Collectors.toSet());
            for (OWLClassExpression c1 : subClassOfAxiomSet.stream().map(OWLSubClassOfAxiom::getSubClass).collect(Collectors.toSet())){
                Set<OWLObjectAllValuesFrom> superClasses = subClassOfAxiomSet.stream()
                        .filter(subClassOfAxiom -> subClassOfAxiom.getSubClass().equals(c1))
                        .map(subClassOfAxiom -> ((OWLObjectAllValuesFrom)subClassOfAxiom.getSuperClass()))
                        .collect(Collectors.toSet());
                Set<OWLClassExpression> possibleC2 = superClasses.stream().filter(ax -> ax.getProperty().equals(r1)).collect(Collectors.toSet());
                Set<OWLClassExpression> possibleC3 = superClasses.stream().filter(ax -> ax.getProperty().equals(r2)).collect(Collectors.toSet());
                if(!possibleC2.isEmpty() && !possibleC3.isEmpty()){
                    return Optional.of(dataFactory.getOWLDisjointClassesAxiom(possibleC2.iterator().next(), possibleC3.iterator().next()));
                }
            }
            // R1 ⊑ R2, c1 ⊑ ∀R1.c2, Disj(c2, c3) -> c1 ⊑ ∀R2.c3,
            Optional<OWLSubClassOfAxiom> result = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                    .filter(ax -> ((OWLObjectAllValuesFrom) ax.getSuperClass()).getProperty().equals(r1))
                    .flatMap(ax -> {
                        OWLClassExpression c1 = ax.getSubClass();
                        OWLObjectAllValuesFrom allValuesFrom = (OWLObjectAllValuesFrom) ax.getSuperClass();
                        OWLClassExpression c2 = allValuesFrom.getFiller();

                        return ontology.axioms(AxiomType.DISJOINT_CLASSES)
                                .filter(disjointClassesAxiom -> disjointClassesAxiom.getClassExpressions().contains(c2))
                                .flatMap(disjointClassesAxiom -> disjointClassesAxiom.getClassExpressions().stream())
                                .filter(c3 -> !c1.equals(c2) && !c2.equals(c3) && !c1.equals(c3))
                                .map(c3 -> dataFactory.getOWLSubClassOfAxiom(c1, dataFactory.getOWLObjectAllValuesFrom(r2, c3)));
                    })
                    .findFirst();

            if(result.isPresent()) return Optional.of(result.get());
        }
        //  if c1 ⊑ ∀R1.c2, c1 ⊑ ∀R2.c3, Disj(c2, c3) in ontology -> insert R1 ⊑ R2
        Set<OWLClass> classes = ontology.classesInSignature().collect(HashSet::new, Set::add, Set::addAll);
        for (OWLClass c1 : classes) {
            Set<OWLClassExpression> allValuesFromRestrictions = new HashSet<>();
            OWLClassExpression c2 = null, c3 = null;
            OWLObjectProperty r1 = null, r2 = null;

            // Identify axioms of the form c1 ⊑ ∀R1.c2 and c1 ⊑ ∀R2.c3
            for (OWLAxiom axiom : ontology.getAxioms(c1)) {
                if (axiom instanceof OWLSubClassOfAxiom subClassAxiom) {
                    OWLClassExpression superClass = subClassAxiom.getSuperClass();

                    if (superClass instanceof OWLObjectAllValuesFrom allValuesFrom) {
                        OWLObjectPropertyExpression property = allValuesFrom.getProperty();
                        OWLClassExpression filler = allValuesFrom.getFiller();

                        if (c2 == null) {
                            c2 = filler;
                            r1 = (OWLObjectProperty) property;
                        } else if (c3 == null) {
                            c3 = filler;
                            r2 = (OWLObjectProperty) property;
                        }
                        allValuesFromRestrictions.add(filler);
                    }
                }
            }
            if (c2 != null && c3 != null && r1 != null && r2 != null) {
                for (OWLDisjointClassesAxiom disjointAxiom : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
                    if (disjointAxiom.contains(c2) && disjointAxiom.contains(c3)) {
                        // Print inferred role subsumption R1 ⊑ R2
                        return Optional.of(dataFactory.getOWLSubObjectPropertyOfAxiom(r1,r2));
                    }
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public String getName() {
        return "OILWPI";
    }
}


