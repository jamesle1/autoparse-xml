package com.workday.autoparse.xml.codegen;

import com.workday.autoparse.xml.annotations.XmlParserPartition;
import com.workday.autoparse.xml.annotations.codegen.XmlParser;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.Set;

class Log {

    static void printElements(ProcessingEnvironment processingEnv, String message, Set<? extends Element> elements) {
        StringBuilder log = new StringBuilder("\n");
        for (Element element : elements) {
            log.append(element.getSimpleName().toString()).append("\n");
        }
        println(processingEnv, message + log + "--- end of elements ---");
    }

    static void printXmlParsers(ProcessingEnvironment processingEnv, String message, Set<XmlParser> parsers) {
        StringBuilder log = new StringBuilder("\n");
        for (XmlParser parser : parsers) {
            log.append(parser).append("\n");
        }
        println(processingEnv, message + log + "--- end of parsers ---");
    }

    static void printXmlParserPartitions(ProcessingEnvironment processingEnv, String message, Set<XmlParserPartition> parsers) {
        StringBuilder log = new StringBuilder("\n");
        for (XmlParserPartition parser : parsers) {
            log.append(parser).append("\n");
        }
        println(processingEnv, message + log + "--- end of partitions ---");
    }

    static void println(ProcessingEnvironment processingEnv, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "\n" + message);
    }
}
