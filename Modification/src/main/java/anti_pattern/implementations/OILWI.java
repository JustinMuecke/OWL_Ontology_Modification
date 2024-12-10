package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import anti_pattern.Util;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OILWI implements Anti_Pattern {

    private final OWLDataFactory dataFactory;
    public OILWI() {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        dataFactory = manager.getOWLDataFactory();
    }

    /**
     * c1⊑c2, c1⊑∀R.c3, c2⊑∀R.c4, Disj(c3, c4)
     * @param ontology ontology on which to perform search
     * @return the Axiom which can be injected into the ontology if found, else it Optional.empty
     */
    @Override
    public Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology) {
        //a1: c1⊑c2, a2: c1⊑∀R.c3, a3: c2⊑∀R.c4 in ontology -> insert Disj(c3,c4)

        for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression c2 = axiom.getSuperClass();
            if (c1.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)
                    && c2.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {

                Set<OWLClassExpression> possibleC3 = Util.findFillersOfObjectAllValuesFromAxioms(ontology, c1, c2);
                Set<OWLClassExpression> possibleC4 = Util.findFillersOfObjectAllValuesFromAxioms(ontology, c2, c1);
                if(possibleC3.isEmpty() && possibleC4.isEmpty()) continue;
                OWLClassExpression c3 = possibleC3.iterator().next();
                OWLClassExpression c4 = possibleC4.iterator().next();
                return Optional.of(List.of(dataFactory.getOWLDisjointClassesAxiom(c3,c4)));

            }
        }
        //a1: c1⊑c2, a2.1: c1⊑∀R.c3, a3: Disj(c3,c4) in ontology -> insert c2⊑∀R.c4
        //           a2.2: c2⊑∀R.c4                              -> insert c1⊑∀R.c3
        for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            OWLClassExpression c1 = axiom.getSubClass();
            OWLClassExpression c2 = axiom.getSuperClass();
            if (c1.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)
                    && c2.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
                //a2.1
                Optional<OWLSubClassOfAxiom> injection = Util.findPossibleInjectionBasedOnSubClassAxiom(ontology, c2, c1, dataFactory);
                if (injection.isPresent()) {
                    return Optional.of(List.of(injection.get()));
                }
                //a2.2
                injection = Util.findPossibleInjectionBasedOnSubClassAxiom(ontology, c1, c2, dataFactory);
                if (injection.isPresent()) {
                    return Optional.of(List.of(injection.get()));
                }
            }

        }
        // c1⊑∀R.c3, c2⊑∀R.c4, Disj(c3, c4) in ontology -> insert c1⊑c2
        for (OWLDisjointClassesAxiom axiom : ontology.getAxioms(AxiomType.DISJOINT_CLASSES)) {
            Set<OWLClassExpression> classes = axiom.getClassExpressions();
            Stream<OWLSubClassOfAxiom> axiomStream = ontology.axioms(AxiomType.SUBCLASS_OF)
                    .filter(subClassOfAxiom -> subClassOfAxiom.getSuperClass().getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM));
            Set<OWLSubClassOfAxiom> possibleSubClassAxioms = axiomStream.filter(ax -> classes.contains(((OWLObjectAllValuesFrom) ax.getSuperClass()).getFiller()))
                    .collect(Collectors.toSet());
            for (OWLSubClassOfAxiom possibleSubClassAxiom : possibleSubClassAxioms) {
                OWLObjectPropertyExpression property = ((OWLObjectAllValuesFrom) possibleSubClassAxiom.getSuperClass()).getProperty();
                OWLClassExpression c3 = possibleSubClassAxiom.getSubClass();
                Set<OWLSubClassOfAxiom> foundPattern = possibleSubClassAxioms.stream().filter(ax -> !ax.equals(possibleSubClassAxiom))
                        .filter(ax -> ((OWLObjectAllValuesFrom) ax.getSuperClass()).getProperty().equals(property))
                        .filter(ax -> !ax.getSubClass().equals(c3))
                        .filter(ax -> !((OWLObjectAllValuesFrom) ax.getSuperClass()).getFiller().equals(((OWLObjectAllValuesFrom) possibleSubClassAxiom).getFiller()))
                        .map(ax -> ((OWLObjectAllValuesFrom) ax.getSuperClass()).getFiller())
                        .map(cls -> dataFactory.getOWLSubClassOfAxiom(c3, cls))
                        .collect(Collectors.toSet());
                if (!foundPattern.isEmpty()) {
                    return Optional.of(List.of(foundPattern.stream().iterator().next()));
                }
            }

        }
        return Optional.empty();
    }






    @Override
    public String getName() {
        return "OILWI";
    }
}
