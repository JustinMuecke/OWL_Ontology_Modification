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
        File[] dir = new File(INPUT_PATH).listFiles();
        File outputDir = new File (OUTPUT_PATH);
        File parsing_error = new File("output/parsing.csv");

        if(!outputDir.exists()){
            outputDir.mkdirs();
        }
        System.out.println(System.getProperty("user.dir"));
        for(File file : dir){
            if(file.getName().equals(".gitkeep")) continue;
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = null;
            try {
                ontology = manager.loadOntologyFromOntologyDocument(file);
            } catch (Exception e) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(parsing_error))) {
                    bw.write(file.getName());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (ontology == null) continue;


            possibleInjections = new HashMap<>();
            for(Anti_Pattern pattern : consideredAntiPattern){
                System.out.println(pattern.getName());
                Optional<OWLAxiom> injectablePattern = pattern.checkForPossiblePatternCompletion(ontology);
                System.out.println(injectablePattern.toString());
                if(injectablePattern.isPresent()) {
                    possibleInjections.put(pattern.getName(), injectablePattern.get());
                }
            }
            if(possibleInjections.isEmpty()) continue;
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
                manager.saveOntology(ontology, IRI.create(new File(outputDir, chosenPattern + "_"+file.getName()+"_").toURI()));
                System.out.println("Successfully saved Ontology: " + file.getName());
            }
            catch(OWLOntologyStorageException storageException){
                System.err.println("Failed to save ontology: " + file.getName() + " due to " + storageException.getMessage());
            }
            break;
        }


    }
}
