package com.mycompany.myshop.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The dependency rule, executable. This is the definition of done for the clean-architecture
 * refactor, not a review checklist: a framework import that reaches the centre fails the build.
 *
 * <p>Rules are enabled as the refactor lands. Everything below is live except the two marked
 * {@link ArchIgnore}, which widen at Step 9 once the use case classes are wired as explicit beans
 * instead of being {@code @Service}-scanned.
 */
@AnalyzeClasses(
        packages = "com.mycompany.myshop.backend",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String USECASES = "..usecases..";
    private static final String PRESENTATION = "..presentation..";
    private static final String INFRASTRUCTURE = "..infrastructure..";

    private static final String SPRING = "org.springframework..";
    private static final String LOMBOK = "lombok..";

    /**
     * The centre is framework-free. No Spring, no JPA, no Jackson, no Lombok, no Hibernate — the
     * domain is plain Java, so it can be unit-tested with no context and no Docker.
     */
    @ArchTest
    static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    SPRING,
                    "jakarta..",
                    "com.fasterxml.jackson..",
                    LOMBOK,
                    "org.hibernate..")
            .because("the domain is the centre: dependencies point inward, and it depends on nothing");

    /** The centre knows nothing about the rings around it. */
    @ArchTest
    static final ArchRule DOMAIN_DEPENDS_ON_NOTHING_OUTSIDE = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(USECASES, PRESENTATION, INFRASTRUCTURE)
            .because("the domain may not know about use cases, the web, or any adapter");

    /** Use cases know the domain, never the delivery mechanism or the adapters. */
    @ArchTest
    static final ArchRule USECASES_DEPEND_ONLY_INWARD = noClasses()
            .that().resideInAPackage(USECASES)
            .should().dependOnClassesThat().resideInAnyPackage(PRESENTATION, INFRASTRUCTURE)
            .because("a use case talks to ports declared in the domain, not to their implementations");

    /** Controllers reach the domain through use cases; they never touch an adapter directly. */
    @ArchTest
    static final ArchRule PRESENTATION_DOES_NOT_REACH_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage(PRESENTATION)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
            .because("the web layer depends on use cases and the domain, never on an adapter");

    /** The ORM is an implementation detail of one package. */
    @ArchTest
    static final ArchRule PERSISTENCE_IS_CONFINED_TO_INFRASTRUCTURE = noClasses()
            .that().resideOutsideOfPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
            .because("JPA entities and their mapping live in infrastructure.persistence only");

    /** So is Spring Data. */
    @ArchTest
    static final ArchRule SPRING_DATA_IS_CONFINED_TO_INFRASTRUCTURE = noClasses()
            .that().resideOutsideOfPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAPackage("org.springframework.data..")
            .because("repository interfaces in the domain are plain Java; Spring Data implements them from outside");

    /**
     * Step 9 widens the rule to here. {@code usecases} keeps exactly one framework import —
     * {@code jakarta.validation} on the request DTOs, allowed by name per D9 so it reads as a
     * decision rather than an oversight — and loses Spring and Lombok when the use case classes
     * become explicitly-wired beans.
     */
    @ArchTest
    @ArchIgnore(reason = "Enabled by Step 9: use cases are still @Service-scanned and DTOs still use Lombok")
    static final ArchRule USECASES_ARE_FRAMEWORK_FREE_EXCEPT_JAKARTA_VALIDATION = noClasses()
            .that().resideInAPackage(USECASES)
            .should().dependOnClassesThat().resideInAnyPackage(SPRING, LOMBOK)
            .because("only jakarta.validation is an accepted framework import in the use case layer (D9)");

    /**
     * Step 9 widens the rule to here too: Jackson is a wire concern, so it belongs to the adapters
     * and the web layer, never to the use case DTOs.
     */
    @ArchTest
    @ArchIgnore(reason = "Enabled by Step 9: PlaceOrderResponse still carries @JsonIgnoreProperties")
    static final ArchRule JACKSON_IS_CONFINED_TO_THE_OUTSIDE = noClasses()
            .that().resideInAnyPackage(DOMAIN, USECASES)
            .should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson..")
            .because("serialization is an outer-ring concern");
}
