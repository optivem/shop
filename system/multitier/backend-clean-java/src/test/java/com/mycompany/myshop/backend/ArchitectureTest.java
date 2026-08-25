package com.mycompany.myshop.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;

import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCoupons;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistory;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetails;
import com.mycompany.myshop.backend.usecases.report.ViewSalesReport;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.mycompany.myshop.backend",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String USECASES = "..usecases..";
    private static final String[] USECASE_PACKAGES =
            {"..usecases.order..", "..usecases.coupon..", "..usecases.report.."};
    private static final String QUERIES = "..usecases.queries..";
    private static final String PRESENTATION = "..presentation..";
    private static final String INFRASTRUCTURE = "..infrastructure..";

    private static final String SPRING = "org.springframework..";
    private static final String LOMBOK = "lombok..";

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

    @ArchTest
    static final ArchRule DOMAIN_DEPENDS_ON_NOTHING_OUTSIDE = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(USECASES, PRESENTATION, INFRASTRUCTURE)
            .because("the domain may not know about use cases, the web, or any adapter");

    @ArchTest
    static final ArchRule USECASES_DEPEND_ONLY_INWARD = noClasses()
            .that().resideInAPackage(USECASES)
            .should().dependOnClassesThat().resideInAnyPackage(PRESENTATION, INFRASTRUCTURE)
            .because("a use case talks to ports declared in the domain, not to their implementations");

    @ArchTest
    static final ArchRule PRESENTATION_DOES_NOT_REACH_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage(PRESENTATION)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
            .because("the web layer depends on use cases and the domain, never on an adapter");

    @ArchTest
    static final ArchRule PERSISTENCE_IS_CONFINED_TO_INFRASTRUCTURE = noClasses()
            .that().resideOutsideOfPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
            .because("JPA entities and their mapping live in infrastructure.persistence only");

    @ArchTest
    static final ArchRule SPRING_DATA_IS_CONFINED_TO_INFRASTRUCTURE = noClasses()
            .that().resideOutsideOfPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAPackage("org.springframework.data..")
            .because("repository interfaces in the domain are plain Java; Spring Data implements them from outside");

    @ArchTest
    static final ArchRule USECASES_ARE_FRAMEWORK_FREE_EXCEPT_JAKARTA_VALIDATION = noClasses()
            .that().resideInAPackage(USECASES)
            .should().dependOnClassesThat().resideInAnyPackage(SPRING, LOMBOK)
            .because("only jakarta.validation is an accepted framework import in the use case layer (D9)");

    @ArchTest
    static final ArchRule USECASES_IMPLEMENT_THE_USECASE_INTERFACE = classes()
            .that().resideInAnyPackage(USECASE_PACKAGES)
            .and().haveSimpleNameNotEndingWith("Request")
            .and().haveSimpleNameNotEndingWith("Response")
            .should().implement(UseCase.class)
            .because("every use case has the same signature: one request in, one declared Result out");

    @ArchTest
    static final ArchRule REQUESTS_AND_RESPONSES_LIVE_WITH_THEIR_USECASE = classes()
            .that().resideInAPackage(USECASES)
            .and(simpleNameEndingWith("Request").or(simpleNameEndingWith("Response")))
            .should().resideInAnyPackage(USECASE_PACKAGES)
            .because("a request or response belongs to one use case, so it lives beside it");

    // Light CQRS, made executable. A pure query -- one whose response holds no field the database
    // does not already hold -- answers with stored columns, so it never builds the domain model and
    // therefore can never fail on a write-side invariant. Before this rule, BrowseCoupons imported
    // Coupon and CouponRepository and one row with a zero discount rate failed the whole list.
    @ArchTest
    static final ArchRule READ_USECASES_DO_NOT_TOUCH_THE_DOMAIN = noClasses()
            .that().resideInAPackage(QUERIES)
            .or().haveFullyQualifiedName(BrowseCoupons.class.getName())
            .or().haveFullyQualifiedName(BrowseOrderHistory.class.getName())
            .or().haveFullyQualifiedName(ViewOrderDetails.class.getName())
            .or().haveFullyQualifiedName(ViewSalesReport.class.getName())
            .should().dependOnClassesThat().resideInAPackage(DOMAIN)
            .because("a pure query reports what is stored; it does not re-run the rules that wrote it");

    @ArchTest
    static final ArchRule JACKSON_IS_CONFINED_TO_THE_OUTSIDE = noClasses()
            .that().resideInAnyPackage(DOMAIN, USECASES)
            .should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson..")
            .because("serialization is an outer-ring concern");

    // OrderJpaRepository.updateStatus writes one column, because status is the only field an existing
    // Order can differ from its stored row in. That is a comment over there and an assumption here,
    // and nothing else would notice it breaking: give Order a second mutable field and the code still
    // compiles, the tests still pass, and every write of that field is silently dropped. This rule is
    // what notices. If it fails, the fix is not to relax it -- it is to widen the UPDATE to match.
    @ArchTest
    static final ArchRule STATUS_IS_THE_ONLY_MUTABLE_FIELD_ON_ORDER = fields()
            .that().areDeclaredIn(Order.class)
            .and().areNotStatic()
            .and().haveNameNotMatching("status")
            .should().beFinal()
            .because("updating an order is updating its status, and the UPDATE statement says so");
}
