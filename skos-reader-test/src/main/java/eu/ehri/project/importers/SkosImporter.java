package eu.ehri.project.importers;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.skos.*;
import org.semanticweb.skosapibinding.SKOSManager;
import uk.ac.manchester.cs.skos.SKOSRDFVocabulary;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SkosImporter {

    public static final String SKOS_BASE = "http://www.w3.org/2004/02/skos/core#";
    public static final URI PREF_LABEL = URI.create(SKOS_BASE + "prefLabel");
    public static final URI ALT_LABEL = URI.create(SKOS_BASE + "altLabel");
    public static final URI SCOPE_NOTE = URI.create(SKOS_BASE + "scopeNote");
    public static final URI DEFINITION = URI.create(SKOS_BASE + "definition");
    public static final URI CONCEPT = URI.create(SKOS_BASE + "Concept");
    public static final URI BROADER = URI.create(SKOS_BASE + "broader");
    public static final URI NARROWER = URI.create(SKOS_BASE + "narrower");
    public static final URI RELATION = URI.create(SKOS_BASE + "relation");

    public static void owlMain(String file) throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(file));

        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLClass conceptClass = factory.getOWLClass(IRI.create(CONCEPT));

        for (OWLClassAssertionAxiom ax : ontology.getClassAssertionAxioms(conceptClass)) {
            OWLNamedIndividual item = ax.getIndividual().asOWLNamedIndividual();
            Set<OWLAnnotationAssertionAxiom> assertionAxioms = item.getAnnotationAssertionAxioms(ontology);
            URI uri = item.getIRI().toURI();
            System.out.println(uri);
            for (OWLAnnotationAssertionAxiom axiom : assertionAxioms) {
                System.out.println(" - " + axiom.getProperty() + " -> " + axiom.getValue());
            }

            OWLAnnotationProperty prelLabelProp = factory
                    .getOWLAnnotationProperty(IRI.create(PREF_LABEL));
            System.out.println("   pref: " + prelLabelProp);
            item.getAnnotations(ontology, prelLabelProp);
            for (OWLAnnotation property : item.getAnnotations(ontology, prelLabelProp)) {
                OWLAnnotationValue value = property.getValue();
                if (value instanceof OWLLiteral) {
                    OWLLiteral literal = (OWLLiteral)property.getValue();
                    System.out.println("  -> " + literal.getLiteral() + " = " + literal.getLang());
                }
            }

            OWLAnnotationProperty relProp = factory.getOWLAnnotationProperty(IRI.create(NARROWER));
            for (OWLAnnotation property : item.getAnnotations(ontology, relProp)) {
                OWLAnnotationValue value = property.getValue();
                System.out.println(" + related: " + URI.create(value.toString()));
            }


        }
    }

    public static void jena(String file) throws Exception {
        OntModel model = ModelFactory.createOntologyModel();
        InputStream in = FileManager.get().open(file);
        model.read(in, null);
        OntClass ontClass = model.getOntClass(CONCEPT.toString());

        ExtendedIterator<? extends OntResource> extendedIterator = ontClass.listInstances();

        final ObjectProperty prefLabelProperty = model.getObjectProperty(PREF_LABEL.toString());
        final ObjectProperty broaderProperty = model.getObjectProperty(BROADER.toString());

        while (extendedIterator.hasNext()) {
            Resource resource = extendedIterator.next();
            System.out.println(resource.getURI());
            Statement property = resource.getProperty(prefLabelProperty);
            RDFNode object = property.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                System.out.println(" - " + literal.getString() + " = " + literal.getLanguage());
            }

            StmtIterator stmtIterator = resource.listProperties();
            List<Statement> statements = stmtIterator.filterKeep(new Filter<Statement>() {
                @Override
                public boolean accept(Statement statement) {
                    return statement.getPredicate().getURI().equals(BROADER.toString());
                }
            }).toList();
            for (Statement statement : statements) {
                System.out.println(" - broader: " + statement.getPredicate() + " -> " +  statement.getObject());
            }

            StmtIterator stmtIterator2 = resource.listProperties();
            List<Statement> statements2 = stmtIterator2.filterKeep(new Filter<Statement>() {
                @Override
                public boolean accept(Statement statement) {
                    return statement.getPredicate().getURI().equals(NARROWER.toString());
                }
            }).toList();
            for (Statement statement : statements2) {
                System.out.println(" - narrower: " + statement.getPredicate() + " -> " +  statement.getObject());
            }
        }
    }


    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: tool [skos-file]");
        }
        jena(args[0]);
    }

    public static void skos(String file) throws Exception {

        SKOSManager manager  = new SKOSManager();
        SKOSDataset vocab = manager.loadDatasetFromPhysicalURI(new File(file).toURI());

        Set<SKOSConceptScheme> skosConceptSchemes = vocab.getSKOSConceptSchemes();

        for (SKOSConceptScheme scheme : skosConceptSchemes) {
            System.out.println("Scheme: " + scheme.getURI());
            System.out.println("Concepts found: " + vocab.getSKOSConcepts());

            for (SKOSConcept concept : vocab.getSKOSConcepts()) {
                System.out.println(concept.getURI());
                for (SKOSAnnotation prefLabel : concept.getSKOSAnnotationsByURI(vocab,
                        SKOSRDFVocabulary.PREFLABEL.getURI())) {
                    SKOSLiteral annotationValueAsConstant = prefLabel.getAnnotationValueAsConstant();
                    String lang = annotationValueAsConstant.getAsSKOSUntypedLiteral().getLang();
                    System.out.println(" prefLabel: (" + lang + ") " + annotationValueAsConstant.getLiteral());
                }

                for (SKOSAnnotation altLabel : concept.getSKOSAnnotationsByURI(vocab,
                        SKOSRDFVocabulary.ALTLABEL.getURI())) {
                    SKOSLiteral annotationValueAsConstant = altLabel.getAnnotationValueAsConstant();
                    String lang = annotationValueAsConstant.getAsSKOSUntypedLiteral().getLang();
                    System.out.println(" altLabel: (" + lang + ") " + annotationValueAsConstant.getLiteral());
                }
                for (SKOSAnnotation broaderAnn : concept.getSKOSAnnotationsByURI(vocab,
                        SKOSRDFVocabulary.BROADER.getURI())) {
                    System.out.println(" broaderAnn: " + broaderAnn.getAnnotationValue().getURI());
                }

                for (SKOSAnnotation narrowerAnn : concept.getSKOSAnnotationsByURI(vocab,
                        SKOSRDFVocabulary.NARROWER.getURI())) {
                    System.out.println(" narrowerAnn: " + narrowerAnn.getAnnotationValue().getURI());
                }
                for (SKOSAnnotation relatedAnn : concept.getSKOSAnnotationsByURI(vocab,
                        SKOSRDFVocabulary.RELATED.getURI())) {
                    System.out.println(" relatedAnn: " + relatedAnn.getAnnotationValue().getURI());
                }
            }
        }
    }


}
