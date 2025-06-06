package main;

import anti_pattern.implementations.*;
import anti_pattern.Anti_Pattern;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import database.PostgresDB;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLOutput;
import java.util.*;


public class Main {
    private static final String QUEUE_INPUT = "Modules_Modify";
    private static final String QUEUE_OUTPUT = "Modules_Preprocess";
    private final static String INPUT_PATH = "/input/";
    private final static String OUTPUT_PATH = "/output/";
    private static HashMap<String, List<OWLAxiom>> possibleInjections;
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

    public static void main(String[] args) throws IOException, InterruptedException, SQLException {
        PostgresDB database = new PostgresDB("postgres", "data_processing", "postgres_user", "postgress_password", 5432);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq");
        factory.setUsername("rabbitmq_user");
        factory.setPassword("rabbitmq_password");
        Connection connection = null;

        while (connection == null) {
            try {
                // Attempt to establish a connection and channel
                connection = factory.newConnection();

                System.out.println("Connected to RabbitMQ successfully.");
            } catch (Exception e) {
                System.out.println("Error connecting to RabbitMQ: " + e.getMessage());
                System.out.println("Retrying in 5 seconds...");

                // Sleep for 5 seconds before retrying
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_INPUT, true, false, false, null);
        channel.queueDeclare(QUEUE_OUTPUT, true, false, false, null);

        DeliverCallback deliverCallback = getDeliverCallback(channel, database, connection);
        channel.basicConsume(QUEUE_INPUT, false, deliverCallback, consumerTag -> { });
        System.out.println("Waiting for messages. To exit press CTRL+C");

        while (true) {
            Thread.sleep(1000);
        }
    }


    private static DeliverCallback getDeliverCallback(Channel channel, PostgresDB database, Connection connection) {
        Channel finalChannel = channel;
        return (consumerTag, delivery) -> {
            //Add Database calls

            String filepath = new String(delivery.getBody(), StandardCharsets.UTF_8);
            database.updateStatusInModificationDatabaseStart(filepath);
            System.out.println(filepath +": Execute Injection");

            List<String> newFiles = executeInjection(INPUT_PATH + filepath);
            System.out.println(filepath + ": NEW FILE! " + newFiles);
            if(!newFiles.isEmpty()) {
                database.updateStatusInModificationDatabaseEnd(filepath, newFiles);
                for(String newFile : newFiles){
                    System.out.println(filepath + ": Trying to publish to " + QUEUE_OUTPUT);
                    try {

                        System.out.println(finalChannel.toString());
                        System.out.println("CHANNEL OPEN: " + finalChannel.isOpen());
                        if(!finalChannel.isOpen()) {
                            System.out.println("MAKE NEW CHANNEL");
                            Channel newChannel = connection.createChannel();
                            newChannel.queueDeclare(QUEUE_INPUT, true, false, false, null);
                            newChannel.queueDeclare(QUEUE_OUTPUT, true, false, false, null);
                            System.out.println("NEW CHANNEL OPEN: " + newChannel.isOpen());
                            newChannel.basicPublish("", QUEUE_OUTPUT, null, newFile.getBytes(StandardCharsets.UTF_8));
                            System.out.println("NEW CHANNEL OPEN: " + newChannel.isOpen());
                        }
                        else{
                            finalChannel.basicPublish("", QUEUE_OUTPUT, null, newFile.getBytes(StandardCharsets.UTF_8));
                            System.out.println("CHANNEL OPEN AFTER: " + finalChannel.isOpen());
                        }

                    }catch(IOException e){
                        e.printStackTrace();
                    }
                    System.out.println(filepath +": Published");
                    database.updasteStatusInPreprocessingDatabase(newFile);
                    System.out.println(filepath +": Database Updated");
                }
            }
            else{
                System.out.println(filepath +": Nothing Injected!");
            }
            System.out.println("Received file: " + filepath);
            // Acknowledge the message
            finalChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };
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
            RDFXMLDocumentFormat format = new RDFXMLDocumentFormat();

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
