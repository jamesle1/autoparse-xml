/*
 * Copyright 2015 Workday, Inc.
 *
 * This software is available under the MIT license.
 * Please see the LICENSE.txt file in this project.
 */

package com.workday.autoparse.xml.codegen;

import com.workday.autoparse.xml.annotations.XmlElement;
import com.workday.autoparse.xml.annotations.XmlParserPartition;
import com.workday.autoparse.xml.annotations.codegen.XmlParser;
import com.workday.autoparse.xml.utils.CollectionUtils;
import com.workday.autoparse.xml.utils.StringUtils;
import com.workday.meta.PackageTree;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 * A {@link javax.annotation.processing.Processor} that generates code used to inflate objects
 * annotated with {@link XmlParser}. This processor will be invoked during compilation. Clients
 * will have no need to instantiate or make calls to this class.
 *
 * @author nathan.taylor
 * @since 2013-09-30
 */
public class XmlElementParserMapProcessor extends AbstractProcessor {

    private Map<String, Collection<TypeElement>> parserMap = new HashMap<>();

    Set<PackageElement> partitionPackageElements = new HashSet<>();

    static int test = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Log.println(processingEnv, "\n ---------- xml element parser map processor is called with : " + test++ + "  ---------- ");

        Log.println(processingEnv, "\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        Log.printElements(processingEnv, "\n###### what is root elements: ", roundEnv.getRootElements());
        Log.println(processingEnv, "\n ###############  ################### ");
        Log.printElements(processingEnv, "\n###### what is xml parsers: ", roundEnv.getElementsAnnotatedWith(XmlParser.class));

        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(XmlParser.class);
        if (elements.isEmpty()) {
            return false;
        }
        for (Element e : elements) {
            addClassToParseMap((TypeElement) e);
        }

        final Element element = elements.iterator().next();
        Set<XmlParser> parsers = element.getEnclosingElement()
                                        .getEnclosedElements()
                                        .stream()
                                        .filter(element1 -> element1.getKind() == ElementKind.CLASS)
                                        .filter(element1 -> element1.getAnnotation(XmlParser.class) != null)
                                        .map((Function<Element, XmlParser>) element1 -> element1.getAnnotation(XmlParser.class))
                                        .collect(Collectors.toSet());
        Log.printXmlParsers(processingEnv, "\n!@#$!@#$!@#$!@#$!@#$: printing elements:", parsers);

        final Set<Element> parserElements = element.getEnclosingElement()
                                                   .getEnclosedElements()
                                                   .stream()
                                                   .filter(element1 -> element1.getKind() == ElementKind.CLASS)
                                                   .filter(element1 -> element1.getAnnotation(XmlParser.class) != null)
                                                   .collect(Collectors.toSet());
        for (Element parserElement : parserElements) {
            addClassToParseMap((TypeElement) parserElement);
        }

        Element rootPackage = getPackage(element);
        Log.println(processingEnv, "\n ---------------------------> root package: " + rootPackage);
        Name packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName();
        Element anotherPackage = processingEnv.getElementUtils().getPackageElement(packageName.subSequence(0, packageName.toString().lastIndexOf(".")));
        TypeElement packageTypeElement = processingEnv.getElementUtils().getTypeElement(packageName.subSequence(0, packageName.toString().lastIndexOf(".")));
        Log.println(processingEnv, "\n ---------------------------> root root package: " + packageTypeElement);
//        Log.printElements(processingEnv, "\n ---------------------------> root root package contains: ",processingEnv.getElementUtils().getAllMembers(packageTypeElement).stream().collect(Collectors.toSet()));



                PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
        if (packageElement.getAnnotation(XmlParserPartition.class) != null) {
//            partitionPackageElements.addAll(ElementFilter.packagesIn(Collections.singleton(packageElement)));
            partitionPackageElements.add(packageElement);
        }

//        Log.println(processingEnv, "\n !@#$%#$%@#$% whatever element: " + element.getEnclosingElement().getAnnotation(XmlParserPartition.class));
        Log.println(processingEnv, "\n !@#$%#$%@#$% enclosing element: " + processingEnv.getElementUtils().getPackageOf(element));
        Log.println(processingEnv, "\n !@#$%#$%@#$% enclosing enclosing element: " + processingEnv.getElementUtils().getPackageOf(element.getEnclosingElement()).getEnclosingElement());

        Set<XmlParserPartition> parserPartitions = element.getEnclosingElement()
                                                          .getEnclosedElements()
                                                          .stream()
//                                            .filter(element1 -> element1.getKind() == ElementKind.PACKAGE)
                                                          .filter(element1 -> element1.getAnnotation(XmlParserPartition.class) != null)
                                                          .map((Function<Element, XmlParserPartition>) element1 -> element1.getAnnotation(XmlParserPartition.class))
                                                          .collect(Collectors.toSet());
        Log.printXmlParserPartitions(processingEnv, "\n!@#$!@#$!@#$!@#$!@#$: printing partitions:", parserPartitions);

        // Generate ParserMaps
        Set<PackageElement> partitionPackageElementsInRound = ElementFilter.packagesIn(
                roundEnv.getElementsAnnotatedWith(XmlParserPartition.class));
        if (!partitionPackageElementsInRound.isEmpty()) {
            partitionPackageElements = partitionPackageElementsInRound;
        }
        PackageTree packageTree =
                new PackageTree(processingEnv.getElementUtils(), partitionPackageElements);

//        generateParserMaps(packageTree);
        return true;
    }

    private Element getPackage(Element element) {
        Element nextElement = element.getEnclosingElement();

        while (nextElement instanceof PackageElement && nextElement.getEnclosingElement() != null) {
            nextElement = nextElement.getEnclosingElement();
        }

        return nextElement instanceof PackageElement ? nextElement : null;
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return CollectionUtils.newHashSet(XmlParser.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void addClassToParseMap(TypeElement element) {
        XmlParser annotation = element.getAnnotation(XmlParser.class);
        for (String parseKey : annotation.value()) {
            if (StringUtils.isNotEmpty(parseKey)) {
                putInCollectionMap(parserMap, parseKey, element);
            }
        }
    }

    private <K, V> void putInCollectionMap(Map<K, Collection<V>> map, K key, V value) {
        Collection<V> collection = map.computeIfAbsent(key, k -> new HashSet<>());
        collection.add(value);
    }

    private void generateParserMaps(PackageTree packageTree) {
        Map<PackageElement, Map<String, TypeElement>> mapsByPackage = splitParserMap(packageTree);
        for (Map.Entry<PackageElement, Map<String, TypeElement>> entry : mapsByPackage.entrySet()) {
            ParserMapGenerator generator =
                    new ParserMapGenerator(processingEnv, entry.getKey(), entry.getValue());
            try {
                generator.generateParseMap(processingEnv, entry.getValue().values());
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
    }

    private Map<PackageElement, Map<String, TypeElement>> splitParserMap(PackageTree packageTree) {
        Map<PackageElement, Map<String, TypeElement>> mapsByPackage = new HashMap<>();
        for (Map.Entry<String, Collection<TypeElement>> entry : parserMap.entrySet()) {
            String parseKey = entry.getKey();
            for (TypeElement element : entry.getValue()) {
                // A null matching package means that this element goes into the default partition,
                // which is keyed by null here.
                PackageElement matchingPackage = packageTree.getMatchingPackage(element);
                Map<String, TypeElement> map = mapsByPackage.get(matchingPackage);
                if (map == null) {
                    map = new HashMap<>();
                    mapsByPackage.put(matchingPackage, map);
                }
                TypeElement previousValue = map.put(parseKey, element);
                if (previousValue != null) {
                    String packageString = matchingPackage != null
                            ? String.format(Locale.US,
                                            "partition under package '%s'",
                                            matchingPackage.getQualifiedName())
                            : "the default partition";
                    String errorMessage =
                            String.format("%s and %s both tried to map to tag name \"%s\" in %s.",
                                          element.getQualifiedName(),
                                          previousValue.getQualifiedName(),
                                          parseKey,
                                          packageString);
                    processingEnv.getMessager()
                                 .printMessage(Diagnostic.Kind.ERROR, errorMessage, element);
                }
            }
        }
        return mapsByPackage;
    }

}
