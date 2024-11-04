package main;

import anti_pattern.implementations.AIO;
import anti_pattern.Anti_Pattern;
import anti_pattern.implementations.EID;
import anti_pattern.implementations.OIL;
import anti_pattern.implementations.OILWI;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class Main {

    private final static String INPUT_PATH = "../data/ont_modules";
    private final static String OUTPUT_PATH = "../data/modified";
    private static HashMap<String, OWLAxiom> possibleInjections;
    private static List<Anti_Pattern> consideredAntiPattern = new LinkedList<>(List.of(
            new EID(),
            new AIO(),
            new OIL(),
            new OILWI()
            ));

    public static void main(String[] args) {
        File[] dir = new File(INPUT_PATH).listFiles();
        File outputDir = new File (OUTPUT_PATH);
        File parsing_error = new File("output/parsing.csv");

        if(!outputDir.exists()){
            outputDir.mkdirs();
        }

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
                pattern.checkForPossiblePatternCompletion(null)
                        .ifPresent(axiom -> possibleInjections.put(pattern.getName(), axiom));
            }

            // Pick one possible Injection Randomly and apply to ontology.
            List<String> patterns = possibleInjections.keySet().stream().toList();
            int randomIndex = new Random().nextInt(patterns.size());
            String chosenPattern = patterns.get(randomIndex);

            // Inject Chosen Axiom into Ontology
            OWLAxiom injectionAxiom = possibleInjections.get(chosenPattern);
            manager.addAxiom(ontology, injectionAxiom);
            try {
                manager.saveOntology(ontology, IRI.create(new File(outputDir, file.getName()).toURI()));
                System.out.println("Successfully saved Ontology: " + file.getName());
            }
            catch(OWLOntologyStorageException storageException){
                System.err.println("Failed to save ontology: " + file.getName() + " due to " + storageException.getMessage());
            }
        }


    }
}
