package main;

import anti_pattern.implementations.*;
import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class Main {

    private final static String INPUT_PATH = "../../data/ont_modules";
    private final static String OUTPUT_PATH = "../../data/modified";
    private static HashMap<String, OWLAxiom> possibleInjections;
    private static final List<Anti_Pattern> consideredAntiPattern = new LinkedList<>(List.of(
            new EID(),
            new AIO(),
            new OIL(),
            new OILWI(),
            new OILWPI(),
            new SOSINETO(),
            new UE(),
            new UEWI1(),
            new UEWI2(),
            new UEWIP(),
            new UEWPI()
            ));

    public static void main(String[] args) {


    }

    private static OWLOntology loadOntology(File file, OWLOntologyManager manager){
        File parsingError = new File("output/parsing.csv");
        OWLOntology ontology = null;
        try {
            ontology = manager.loadOntologyFromOntologyDocument(file);
        } catch (Exception e) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(parsingError))) {
                bw.write(file.getName());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return ontology;
    }
    public static String executeInjection(String filepath){
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File outputDir = new File (OUTPUT_PATH);
        File file = new File(filepath);
        OWLOntology ontology = loadOntology(file, manager);
        if(ontology == null) return "";
        possibleInjections = new HashMap<>();
        for(Anti_Pattern pattern : consideredAntiPattern){
            System.out.println(pattern.getName());
            Optional<OWLAxiom> injectablePattern = pattern.checkForPossiblePatternCompletion(ontology);
            System.out.println(injectablePattern.toString());
            injectablePattern.ifPresent(owlAxiom -> possibleInjections.put(pattern.getName(), owlAxiom));
        }
        if(possibleInjections.isEmpty()) return "";
        // Pick one possible Injection Randomly and apply to ontology.
        List<String> patterns = possibleInjections.keySet().stream().toList();
        String chosenPattern = "";
        if(patterns.size()>1) {
            int randomIndex = new Random().nextInt(patterns.size());
            chosenPattern = patterns.get(randomIndex);
        }
        else{
            chosenPattern = patterns.get(0);
        }
        // Inject Chosen Axiom into Ontology
        OWLAxiom injectionAxiom = possibleInjections.get(chosenPattern);
        manager.addAxiom(ontology, injectionAxiom);
        try {
            manager.saveOntology(ontology, IRI.create(new File(outputDir, chosenPattern + "_"+file.getName()).toURI()));
            System.out.println("Successfully saved Ontology: " + file.getName());
            return chosenPattern + "_" + file.getName();
        }
        catch(OWLOntologyStorageException storageException){
            System.err.println("Failed to save ontology: " + file.getName() + " due to " + storageException.getMessage());
            return "";
        }
    }
}
