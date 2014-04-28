package eu.ehri.project.importers;

import org.semanticweb.skos.*;
import org.semanticweb.skosapibinding.SKOSManager;

import java.io.File;
import java.net.URI;
import java.util.Set;

/**
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class SkosImporter {

    public static final String SKOS_BASE = "http://www.w3.org/2004/02/skos/core#";
    public static final URI PREF_LABEL = URI.create(SKOS_BASE + "prefLabel");
    public static final URI ALT_LABEL = URI.create(SKOS_BASE + "altLabel");

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: tool [skos-file]");
        }
        String filePath = args[0];


        SKOSManager manager  = new SKOSManager();
        SKOSDataset vocab = manager.loadDatasetFromPhysicalURI(new File(filePath).toURI());

        Set<SKOSConceptScheme> skosConceptSchemes = vocab.getSKOSConceptSchemes();

        for (SKOSConceptScheme scheme : skosConceptSchemes) {
            System.out.println("Scheme: " + scheme.getURI());
            System.out.println("Concepts found: " + vocab.getSKOSConcepts());

            for (SKOSConcept concept : vocab.getSKOSConcepts()) {
                System.out.println(concept.getURI());
                for (SKOSAnnotation prefLabel : concept.getSKOSAnnotationsByURI(vocab, PREF_LABEL)) {
                    SKOSLiteral annotationValueAsConstant = prefLabel.getAnnotationValueAsConstant();
                    String lang = annotationValueAsConstant.getAsSKOSUntypedLiteral().getLang();
                    System.out.println(" prefLabel: (" + lang + ") " + annotationValueAsConstant.getLiteral());
                }

                for (SKOSAnnotation altLabel : concept.getSKOSAnnotationsByURI(vocab, ALT_LABEL)) {
                    SKOSLiteral annotationValueAsConstant = altLabel.getAnnotationValueAsConstant();
                    String lang = annotationValueAsConstant.getAsSKOSUntypedLiteral().getLang();
                    System.out.println(" altLabel: (" + lang + ") " + annotationValueAsConstant.getLiteral());
                }
                for (SKOSAnnotation annotation : concept.getSKOSAnnotations(vocab)) {

                }
            }
        }
    }
}
