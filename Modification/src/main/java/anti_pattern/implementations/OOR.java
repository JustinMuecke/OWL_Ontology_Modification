package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class OOR implements Anti_Pattern {
    private final Random randomPicker;
    private final OWLDataFactory dataFactory;
    private final OWLOntologyManager manager;

    public OOR() {
        this.randomPicker = new Random();
        this.manager = OWLManager.createOWLOntologyManager();
        this.dataFactory = manager.getOWLDataFactory();
    }
    /**
     * Pattern:  c1 = Range(𝑝), 𝑝(o, s), s∈𝑐2, 𝐷𝑖𝑠𝑗(𝑐1, 𝑐2)
     * O
     * @param ontology
     * @return
     */
    @Override
    public Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology) {
        Optional<OWLObjectPropertyRangeAxiom> possibleDomainInjection = findInjectableRangeAxiom(ontology, this.manager);
        if(possibleDomainInjection.isPresent()) return Optional.of(List.of(possibleDomainInjection.get()));

        Optional<OWLDisjointClassesAxiom> possibleDisjointClassesInjection = findInjectableDisjointClassesAxiom(ontology, manager);
        if(possibleDisjointClassesInjection.isPresent()) return Optional.of(List.of(possibleDisjointClassesInjection.get()));

        Optional<OWLClassAssertionAxiom> possibleClassAssertionInjection = findInjectableClassAssertionAxiom(ontology, manager);
        if(possibleClassAssertionInjection.isPresent()) return Optional.of(List.of(possibleClassAssertionInjection.get()));

        Optional<OWLObjectPropertyAssertionAxiom> possiblePropertyAssertionAxiomInjection = findInjectableObjectPropertyAssertionAxiom(ontology, manager);
        if(possiblePropertyAssertionAxiomInjection.isPresent()) return Optional.of(List.of(possiblePropertyAssertionAxiomInjection.get()));
        return Optional.empty();
    }

    /**
     * Pattern:  c1 = Range(𝑝), 𝐷𝑖𝑠𝑗(𝑐1, 𝑐2), s∈𝑐2 -> return 𝑝(o, s)
     * O
     * @param ontology
     * @param manager
     * @return
     */
    private Optional<OWLObjectPropertyAssertionAxiom> findInjectableObjectPropertyAssertionAxiom(OWLOntology ontology, OWLOntologyManager manager){
        Set<OWLObjectPropertyRangeAxiom> rangeAxiomSet = ontology.getAxioms(AxiomType.OBJECT_PROPERTY_RANGE);
        if(rangeAxiomSet.isEmpty()) return Optional.empty();
        Set<OWLDisjointClassesAxiom> disjointClassesAxiomSet = ontology.getAxioms(AxiomType.DISJOINT_CLASSES);
        if(disjointClassesAxiomSet.isEmpty()) return Optional.empty();

        for(OWLObjectPropertyRangeAxiom rangeAxiom: rangeAxiomSet){
            OWLClassExpression possibleC1 = rangeAxiom.getRange();
            OWLObjectPropertyExpression possibleP = rangeAxiom.getProperty();
            Set<OWLDisjointClassesAxiom> possibleDisjointClassesAxioms = disjointClassesAxiomSet.stream().filter(ax -> ax.getClassExpressions().contains(possibleC1)).collect(Collectors.toSet());

            for(OWLDisjointClassesAxiom disjointClassesAxiom : possibleDisjointClassesAxioms){
                Set<OWLClassExpression> possibleC2s = disjointClassesAxiom.getClassExpressions();
                if(!possibleC2s.remove(possibleC1)) continue;
                if(possibleC2s.isEmpty()) continue;

                OWLClassExpression c2 = possibleC2s.stream().findFirst().get();
                Set<OWLNamedIndividual> possibleSubjects = ontology.axioms(AxiomType.CLASS_ASSERTION).filter(ax-> ax.getIndividual().isOWLNamedIndividual())
                        .filter(ax -> ax.getClassExpression().equals(c2))
                        .map(ax -> ax.getIndividual().asOWLNamedIndividual())
                        .collect(Collectors.toSet());
                if(possibleSubjects.isEmpty()) continue;
                return Optional.of(manager.getOWLDataFactory().getOWLObjectPropertyAssertionAxiom( possibleP, ontology.individualsInSignature().findFirst().get(), possibleSubjects.iterator().next()));
            }

        }
        return Optional.empty();
    }

    /**
     * Pattern:  c1 = Range(𝑝), 𝐷𝑖𝑠𝑗(𝑐1, 𝑐2), 𝑝(o, s) -> return s∈𝑐2
     * O
     * @param ontology
     * @param manager
     * @return
     */
    private Optional<OWLClassAssertionAxiom> findInjectableClassAssertionAxiom(OWLOntology ontology, OWLOntologyManager manager){
        Set<OWLObjectPropertyRangeAxiom> rangeAxiomSet = ontology.getAxioms(AxiomType.OBJECT_PROPERTY_RANGE);
        if(rangeAxiomSet.isEmpty()) return Optional.empty();
        Set<OWLObjectPropertyAssertionAxiom> propertyAxiomSet = ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
        if(propertyAxiomSet.isEmpty()) return Optional.empty();

        for(OWLObjectPropertyRangeAxiom propertyDomainAxiom : rangeAxiomSet){
            OWLClassExpression possibleC1 = propertyDomainAxiom.getRange();
            OWLObjectPropertyExpression possibleProperty = propertyDomainAxiom.getProperty();
            Set<OWLObjectPropertyAssertionAxiom> propertyAssertionAxioms = ontology.axioms(AxiomType.OBJECT_PROPERTY_ASSERTION).filter(ax -> ax.getProperty().equals(possibleProperty)).collect(Collectors.toSet());
            if(propertyAssertionAxioms.isEmpty()) continue;

            Set<OWLDisjointClassesAxiom> disjointClassesAxioms = ontology.axioms(AxiomType.DISJOINT_CLASSES).filter(ax -> ax.getClassExpressions().contains(possibleC1)).collect(Collectors.toSet());
            if(disjointClassesAxioms.isEmpty()) continue;

            for(OWLDisjointClassesAxiom disjointClassesAxiom : disjointClassesAxioms){
                Set<OWLClassExpression> possibleC2s = disjointClassesAxiom.getClassExpressions();
                possibleC2s.remove(possibleC1);
                if(possibleC2s.isEmpty()) continue;
                OWLClassExpression possibleC2 = possibleC2s.iterator().next();
                OWLNamedIndividual subject = propertyAssertionAxioms.stream().findFirst().get().getSubject().asOWLNamedIndividual();
                return Optional.of(manager.getOWLDataFactory().getOWLClassAssertionAxiom(possibleC2, subject));
            }
        }
        return Optional.empty();
    }

    /**
     * Pattern:  c1 = Range(𝑝), s∈𝑐2, 𝑝(o, s) -> return 𝐷𝑖𝑠𝑗(𝑐1, 𝑐2)
     * O
     * @param ontology
     * @param manager
     * @return
     */
    private Optional<OWLDisjointClassesAxiom> findInjectableDisjointClassesAxiom(OWLOntology ontology, OWLOntologyManager manager){
        Set<OWLObjectPropertyRangeAxiom> rangeAxiomSet = ontology.getAxioms(AxiomType.OBJECT_PROPERTY_RANGE);
        if(rangeAxiomSet.isEmpty()) return Optional.empty();
        Set<OWLObjectPropertyAssertionAxiom> propertyAxiomSet = ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
        if(propertyAxiomSet.isEmpty()) return Optional.empty();

        for(OWLObjectPropertyRangeAxiom propertyDomainAxiom : rangeAxiomSet){
            OWLObjectPropertyExpression possibleProperty = propertyDomainAxiom.getProperty();
            OWLClassExpression possibleC1 = propertyDomainAxiom.getRange();
            Set<OWLObjectPropertyAssertionAxiom> possibleRelevantPropertyAssertion = propertyAxiomSet.stream().filter(assertionAxiom -> assertionAxiom.getProperty().equals(possibleProperty)).collect(Collectors.toSet());
            if(possibleRelevantPropertyAssertion.isEmpty()) continue;

            for(OWLObjectPropertyAssertionAxiom propertyAssertionAxiom : possibleRelevantPropertyAssertion){
                OWLNamedIndividual possibleSubject = propertyAssertionAxiom.getSubject().asOWLNamedIndividual();
                Set<OWLClassExpression> possibleC2 = ontology.axioms(AxiomType.CLASS_ASSERTION).filter(ax -> ax.getIndividual().equals(possibleSubject)).map(OWLClassAssertionAxiom::getClassExpression).collect(Collectors.toSet());
                if(possibleC2.isEmpty()) continue;
                return Optional.of(manager.getOWLDataFactory().getOWLDisjointClassesAxiom(possibleC1, possibleC2.iterator().next()));
            }
        }
        return Optional.empty();
    }
    /**
     * Pattern:  𝐷𝑖𝑠𝑗(𝑐1, 𝑐2), s∈𝑐2, 𝑝(o, s) -> return c1 = Range(𝑝)
     * O
     * @param ontology
     * @param manager
     * @return
     */
    private Optional<OWLObjectPropertyRangeAxiom> findInjectableRangeAxiom(OWLOntology ontology, OWLOntologyManager manager){
        Set<OWLObjectPropertyAssertionAxiom> propertyAxiomSet = ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
        if(propertyAxiomSet.isEmpty()) return Optional.empty();

        for(OWLObjectPropertyAssertionAxiom propertyAssertionAxiom : propertyAxiomSet){
            OWLObjectProperty property = propertyAssertionAxiom.getProperty().asOWLObjectProperty();
            OWLNamedIndividual subject =propertyAssertionAxiom.getSubject().asOWLNamedIndividual();

            Set<OWLClassExpression> possibleC2 = ontology.axioms(AxiomType.CLASS_ASSERTION).filter(ax -> ax.getIndividual().equals(subject)).map(OWLClassAssertionAxiom::getClassExpression).collect(Collectors.toSet());
            if(possibleC2.isEmpty()) continue;
            Optional<OWLDisjointClassesAxiom> disjointClassesAxiomOpt = ontology.axioms(AxiomType.DISJOINT_CLASSES)
                    .filter(ax -> ax.getClassExpressions().stream().anyMatch(possibleC2::contains))
                    .findFirst();
            if(disjointClassesAxiomOpt.isEmpty()) return Optional.empty();

            OWLDisjointClassesAxiom disjointClassesAxiom = disjointClassesAxiomOpt.get();
            for(OWLClassExpression classExpression: disjointClassesAxiom.getClassExpressions()){
                if(!possibleC2.contains(classExpression)){
                    return Optional.of(manager.getOWLDataFactory().getOWLObjectPropertyRangeAxiom(property, classExpression));
                }
            }
        }
        return Optional.empty();
    }


    @Override
    public String getName() {
        return "OOR";
    }
}
