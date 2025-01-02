package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UEWPI implements Anti_Pattern {
    private final Random randomPicker;
    private final OWLDataFactory dataFactory;

    public UEWPI() {
        randomPicker = new Random();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * R1 ⊑ R2, c1 ⊑ ∃R1.c2, c1 ⊑ ∀R2.c3, Disj(c2, c3)
     * @param ontology
     * @return
     */
    @Override
    public Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology) {

        Optional<OWLDisjointClassesAxiom> disjointResutl = findInjectableDisjointClassAxioms(ontology);
        if(disjointResutl.isPresent()) return Optional.of(List.of(disjointResutl.get()));
        Optional<OWLSubClassOfAxiom> subClassResult = findInjectableSubClassAxiomsWithForAllRestriction(ontology);
        if(subClassResult.isPresent()) return Optional.of(List.of(subClassResult.get()));
        Optional<OWLSubClassOfAxiom> subClassResult2 = findInjectableSubClassAxiomsWithExistsRestriction(ontology);
        if(subClassResult2.isPresent()) return Optional.of(List.of(subClassResult2.get()));

        Optional<OWLSubObjectPropertyOfAxiom> propertyResult=findInjectableSubPropertyAxioms(ontology);
        if(propertyResult.isPresent()) return Optional.of(List.of(propertyResult.get()));
        return Optional.empty();
    }

    /**
     * R1 ⊑ R2, c1 ⊑ ∃R1.c2, c1 ⊑ ∀R2.c3 in ontology -> insert Disj(c2, c3)
     * @param ontology
     * @return
     */
    private Optional<OWLDisjointClassesAxiom> findInjectableDisjointClassAxioms(OWLOntology ontology){
        Set<OWLSubObjectPropertyOfAxiom> subPropertyAxiomSet = ontology.getAxioms(AxiomType.SUB_OBJECT_PROPERTY);
        for(OWLSubObjectPropertyOfAxiom subPropertyAxiom : subPropertyAxiomSet) {
            OWLObjectPropertyExpression r1 = subPropertyAxiom.getSubProperty();
            OWLObjectPropertyExpression r2 = subPropertyAxiom.getSuperProperty();
            // Find c1 -> need c that appears in c1 ⊑ ∃ structure and c1 ⊑ ∀ structure
            // existsSubClasses c1 ⊑ ∃R1
            Set<OWLClassExpression> existsSubClasses = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                    .filter(ax -> ((OWLObjectSomeValuesFrom)ax.getSuperClass()).getProperty().equals(r1))
                    .map(OWLSubClassOfAxiom::getSubClass)
                    .collect(Collectors.toSet());
            // forAllSubClasses c1 ⊑ ∀R2
            Set<OWLClassExpression> forAllSubClass = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                    .filter(ax -> ((OWLObjectSomeValuesFrom)ax.getSuperClass()).getProperty().equals(r2))
                    .map(OWLSubClassOfAxiom::getSubClass)
                    .collect(Collectors.toSet());
            Set<OWLClassExpression> c1Candidates = new HashSet<>(existsSubClasses);
            c1Candidates.retainAll(forAllSubClass);
            // find c2 and c3
            for(OWLClassExpression c1 : c1Candidates){
                OWLClassExpression c2 = null, c3 = null;
                for (OWLSubClassOfAxiom subClassAxiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
                    if(!subClassAxiom.getSubClass().equals(c1)) continue;
                    OWLClassExpression superClass = subClassAxiom.getSuperClass();
                    if (superClass instanceof OWLObjectSomeValuesFrom someValuesFrom && someValuesFrom.getProperty().equals(r1)){
                            c2 = someValuesFrom.getFiller();
                        }

                    if (superClass instanceof OWLObjectAllValuesFrom allValuesFrom && allValuesFrom.getProperty().equals(r2)){
                            c3 = allValuesFrom.getFiller();
                        }

                }
                if(c2 != null && c3 != null && !c2.equals(c3)){
                    return Optional.of(dataFactory.getOWLDisjointClassesAxiom(c2,c3));
                }


            }
        }


        return Optional.empty();
    }

    /**
     * R1 ⊑ R2, c1 ⊑ ∃R1.c2, Disj(c2, c3) in ontology -> insert c1 ⊑ ∀R2.c3
     * @param ontology
     * @return
     */
    private Optional<OWLSubClassOfAxiom> findInjectableSubClassAxiomsWithForAllRestriction(OWLOntology ontology){

        Set<OWLSubObjectPropertyOfAxiom> subPropertyAxiomSet = ontology.getAxioms(AxiomType.SUB_OBJECT_PROPERTY);
        for(OWLSubObjectPropertyOfAxiom subPropertyAxiom : subPropertyAxiomSet) {
            OWLObjectPropertyExpression r1 = subPropertyAxiom.getSubProperty();
            OWLObjectPropertyExpression r2 = subPropertyAxiom.getSuperProperty();

            //find Axioms of the form ... ⊑ ∃R1 ...
            Stream<OWLSubClassOfAxiom> subClassOfExistsRestriction = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                    .filter(ax -> ((OWLObjectSomeValuesFrom) ax.getSuperClass()).getProperty().equals(r1));
            Set<OWLClassExpression> c1Candidates = subClassOfExistsRestriction.map(OWLSubClassOfAxiom::getSubClass).collect(Collectors.toSet());

            for(OWLClassExpression c1 : c1Candidates){
                OWLClassExpression c2 = null;
                for (OWLSubClassOfAxiom subClassAxiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
                    if(!subClassAxiom.getSubClass().equals(c1)) continue;
                    OWLClassExpression superClass = subClassAxiom.getSuperClass();
                    if (superClass instanceof OWLObjectSomeValuesFrom someValuesFrom && someValuesFrom.getProperty().equals(r1)){
                        c2 = someValuesFrom.getFiller();
                        OWLClassExpression finalC = c2;
                        Set<OWLClassExpression> c3Candidates = ontology.axioms(AxiomType.DISJOINT_CLASSES)
                                .map(OWLNaryClassAxiom::getClassExpressions)
                                .filter(classExpressions -> classExpressions.contains(finalC))
                                .reduce(new HashSet<>(), (acc, val) -> {
                                    acc.addAll(val);
                                    return acc;
                                });
                        if(c3Candidates.isEmpty()) continue;
                        for(OWLClassExpression c3 : c3Candidates){
                            if(c3.equals(c1)) continue;
                            return Optional.of(dataFactory.getOWLSubClassOfAxiom(c1, dataFactory.getOWLObjectAllValuesFrom(r2,c3)));
                        }
                    }
                    }
                }
            }
        return Optional.empty();
    }

    /**
     * R1 ⊑ R2, c1 ⊑ ∀R2.c3, Disj(c2, c3) in ontology -> insert c1 ⊑ ∃R1.c2
     * @param ontology
     * @return
     */
    private Optional<OWLSubClassOfAxiom> findInjectableSubClassAxiomsWithExistsRestriction(OWLOntology ontology){
        Set<OWLSubObjectPropertyOfAxiom> subPropertyAxiomSet = ontology.getAxioms(AxiomType.SUB_OBJECT_PROPERTY);
        for(OWLSubObjectPropertyOfAxiom subPropertyAxiom : subPropertyAxiomSet) {
            OWLObjectPropertyExpression r1 = subPropertyAxiom.getSubProperty();
            OWLObjectPropertyExpression r2 = subPropertyAxiom.getSuperProperty();

            //find Axioms of the form ... ⊑ ∀R2 ...
            Stream<OWLSubClassOfAxiom> subClassOfExistsRestriction = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                    .filter(ax -> ((OWLObjectSomeValuesFrom) ax.getSuperClass()).getProperty().equals(r2));
            Set<OWLClassExpression> c1Candidates = subClassOfExistsRestriction.map(OWLSubClassOfAxiom::getSubClass).collect(Collectors.toSet());

            for(OWLClassExpression c1 : c1Candidates){
                OWLClassExpression c2 = null;
                for (OWLSubClassOfAxiom subClassAxiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
                    if(!subClassAxiom.getSubClass().equals(c1)) continue;
                    OWLClassExpression superClass = subClassAxiom.getSuperClass();
                    if (superClass instanceof OWLObjectSomeValuesFrom someValuesFrom && someValuesFrom.getProperty().equals(r2)){
                        c2 = someValuesFrom.getFiller();
                        OWLClassExpression finalC = c2;
                        Set<OWLClassExpression> c3Candidates = ontology.axioms(AxiomType.DISJOINT_CLASSES)
                                .map(OWLNaryClassAxiom::getClassExpressions)
                                .filter(classExpressions -> classExpressions.contains(finalC))
                                .reduce(new HashSet<>(), (acc, val) -> {
                                    acc.addAll(val);
                                    return acc;
                                });
                        if(c3Candidates.isEmpty()) continue;
                        for(OWLClassExpression c3 : c3Candidates){
                            if(c3.equals(c1)) continue;
                            return Optional.of(dataFactory.getOWLSubClassOfAxiom(c1, dataFactory.getOWLObjectSomeValuesFrom(r2,c3)));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * c1 ⊑ ∃R1.c2, c1 ⊑ ∀R2.c3, Disj(c2, c3) in ontology -> insert R1 ⊑ R2
     * @param ontology
     * @return
     */
    private Optional<OWLSubObjectPropertyOfAxiom> findInjectableSubPropertyAxioms(OWLOntology ontology){
        // find all classes that are subclass to a restriction
        // existsSubClasses c1 ⊑ ∃
        Set<OWLClassExpression> existsSubClasses = ontology.axioms(AxiomType.SUBCLASS_OF)
                .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM))
                .map(OWLSubClassOfAxiom::getSubClass)
                .collect(Collectors.toSet());
        // forAllSubClasses c1 ⊑ ∀
        if(existsSubClasses.isEmpty()) return Optional.empty();
        Set<OWLClassExpression> forAllSubClass = ontology.axioms(AxiomType.SUBCLASS_OF)
                .filter(ax -> ax.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM))
                .map(OWLSubClassOfAxiom::getSubClass)
                .collect(Collectors.toSet());
        if(forAllSubClass.isEmpty()) return Optional.empty();

        Set<OWLClassExpression> c1Candidates = new HashSet<>(existsSubClasses);
        c1Candidates.retainAll(forAllSubClass);

        System.out.println(c1Candidates);
        if(c1Candidates.isEmpty()) return Optional.empty();
        // Iterate over them and see if we can find the relations r1 and r2
        for(OWLClassExpression c1 : c1Candidates){
            OWLClassExpression c2 = null, c3 = null;
            OWLObjectPropertyExpression r1 = null, r2 = null;
            //Find c2

            for (OWLSubClassOfAxiom subClassAxiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
                if (!subClassAxiom.getSubClass().equals(c1)) continue;
                OWLClassExpression superClass = subClassAxiom.getSuperClass();
                if (superClass instanceof OWLObjectSomeValuesFrom someValuesFrom) {
                    c2 = someValuesFrom.getFiller();
                    r1 = someValuesFrom.getProperty();
                }
                if (superClass instanceof OWLObjectAllValuesFrom allValuesFrom) {
                    c3 = allValuesFrom.getFiller();
                    r2 = allValuesFrom.getProperty();
                }
                if (c3 != null && c2 != null) {
                    OWLClassExpression finalC = c2;
                    OWLClassExpression finalC1 = c3;
                    if (ontology.axioms(AxiomType.DISJOINT_CLASSES)
                            .map(axiom ->
                                    axiom.getClassExpressions().contains(finalC)
                                            && axiom.getClassExpressions().contains(finalC1))
                            .reduce(false, (acc, val) -> acc || val)
                    ) {
                        return Optional.of(dataFactory.getOWLSubObjectPropertyOfAxiom(r1, r2));
                    }

                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "UEWPI";
    }
}
