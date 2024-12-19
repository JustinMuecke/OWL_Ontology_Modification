package anti_pattern.implementations;

import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CSC implements Anti_Pattern {

    private final OWLDataFactory dataFactory;
    private final OWLOntologyManager manager;

    public CSC() {
        this.manager = OWLManager.createOWLOntologyManager();
        this.dataFactory = manager.getOWLDataFactory();
    }

    /**
     * Pattern: c2 ⊑ c1, c3 ⊑ c2, c1 ⊑ c3
     * @param ontology
     * @return
     */
    @Override
    public Optional<List<OWLAxiom>> checkForPossiblePatternCompletion(OWLOntology ontology) {
        Optional<OWLSubClassOfAxiom> possibleSubClassInjection = findInjectableSubClassAxiom(ontology);
        if(possibleSubClassInjection.isPresent()) return Optional.of(List.of(possibleSubClassInjection.get()));
        return Optional.empty();
    }

    /**
     * If there is a chain of subclasses of the form c2 ⊑ c1 -> c3 ⊑ c2, add cyclic behaviour by adding c1 ⊑ c3
     * @param ontology the OWL ontology in which we look for cyclic behaviour
     * @return the SubClassOfAxiom which needs to be injected if possible
     */
    private Optional<OWLSubClassOfAxiom> findInjectableSubClassAxiom(OWLOntology ontology){
        Set<OWLSubClassOfAxiom> subClassOfAxiomSet = ontology.getAxioms(AxiomType.SUBCLASS_OF);
        for(OWLSubClassOfAxiom subClassOfAxiom : subClassOfAxiomSet){
            Set<OWLClassExpression> possibleC3s = subClassOfAxiomSet.stream()
                    .filter(ax -> !ax.equals(subClassOfAxiom))
                    .filter(ax -> ax.getSuperClass().equals(subClassOfAxiom.getSubClass()))
                    .map(ax -> ax.getSubClass())
                    .collect(Collectors.toSet());
            if(possibleC3s.isEmpty()) continue;
            return Optional.of(dataFactory.getOWLSubClassOfAxiom(subClassOfAxiom.getSuperClass(), possibleC3s.iterator().next()));
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "CSC";
    }
}
