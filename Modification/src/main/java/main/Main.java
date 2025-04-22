package main;

import anti_pattern.implementations.*;
import anti_pattern.Anti_Pattern;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParser;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main {

    private final static List<String> PREFIXES = List.of("EID", "AIO", "OIL", "CSC", "OILWI", "OILWPI", "OOD", "OOR", "SOSINETO", "UE", "UEWI1", "UEWI2", "UEWIP", "UEWPI");
    private final static String INPUT_PATH = "/input/";
    private final static String OUTPUT_PATH = "/output/";
    private static HashMap<String, List<OWLAxiom>> possibleInjections;
    private static final List<Anti_Pattern> consideredAntiPattern = new LinkedList<>(List.of(
            new EID(),
            new AIO(),
            new CSC(),
            new OOR(),
            new OOD(),
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

        // Create a thread pool with a fixed number of threads
        ExecutorService executor = Executors.newFixedThreadPool(4); // Adjust the number based on your system

        // Use a list to collect the future objects of the tasks
        List<Callable<Void>> tasks = new ArrayList<>();

        for (File file : dir) {
            if (PREFIXES.contains(file.getName().split("_")[0])) continue;

            // Add the task to the list of tasks
            tasks.add(() -> {
                try {
                    List<String> fileNames = executeInjection(file.getPath());
                    System.out.println("Created Files: " + fileNames);
                } catch (StackOverflowError overflowError) {
                    System.out.println("Overflow on ontology: " + file.getName());
                }
                return null; // Return value is null, but you could return other types if needed
            });
        }

        try {
            // Submit all tasks to the executor
            executor.invokeAll(tasks);

            // Shut down the executor
            executor.shutdown();
        } catch (InterruptedException e) {
            System.err.println("Execution interrupted: " + e.getMessage());
        }






    }

    private static OWLOntology loadOntology(File file, OWLOntologyManager manager) {
        OWLOntology ontology = null;
        try {
            // Explicitly create the RDFXMLParser
            RDFXMLParser parser = new RDFXMLParser();
            // Define the document source with the file path
            FileDocumentSource documentSource = new FileDocumentSource(file);
            // Create an empty ontology where we will load the data
            ontology = manager.createOntology();
            // Parse the document source with RDF/XML parser
            parser.parse(documentSource.getDocumentIRI(), ontology);

            System.out.println("Ontology " + file.getName() + "loaded successfully.");
        } catch (Exception e) {
            System.out.println("Couldn't load ontology " + file.getPath() + ": " + e.getMessage());
        }
        return ontology;
    }
    public static List<String> executeInjection(String filepath){
        List<String> newFiles = new LinkedList<>();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File outputDir = new File (OUTPUT_PATH);
        File file = new File(filepath);
        OWLOntology ontology = loadOntology(file, manager);
        if(ontology == null) return newFiles;
        possibleInjections = new HashMap<>();
        System.out.println(filepath +": Checking for pattern");
        for(Anti_Pattern pattern : consideredAntiPattern){
            System.out.println("Checking " + pattern.getName());
            Optional<List<OWLAxiom>> injectablePattern = pattern.checkForPossiblePatternCompletion(ontology);
            injectablePattern.ifPresent(owlAxiom -> possibleInjections.put(pattern.getName(), owlAxiom));
        }
        if(possibleInjections.isEmpty()) return newFiles;
        for (String patternName : possibleInjections.keySet()) {
            List<OWLAxiom> injectionAxioms = possibleInjections.get(patternName);

            // Apply the axiom to the ontology
            for(OWLAxiom injectionAxiom : injectionAxioms){
                manager.addAxiom(ontology, injectionAxiom);
            }
            // Set RDF/XML format explicitly
            FunctionalSyntaxDocumentFormat  format = new FunctionalSyntaxDocumentFormat();
            try {
                // Construct the output file path and save the ontology
                File outputFile = new File(outputDir, patternName + "_" + injectionAxioms.size() + "_" + file.getName());
                manager.saveOntology(ontology,
                        format,
                        new FileOutputStream(outputFile));
                newFiles.add(patternName + "_" + file.getName());
                System.out.println("Successfully saved Ontology with pattern " + patternName + ": " + file.getName());
            } catch (OWLOntologyStorageException storageException) {
                System.err.println("Failed to save ontology with pattern " + patternName + ": " + file.getName() + " due to " + storageException.getMessage());
                storageException.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return newFiles;
    }


}
