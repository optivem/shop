package com.mycompany.myshop.systemtest.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * POC: which DSL/driver structural rules can ArchUnit enforce mechanically?
 * Four representative rules, one per feasibility tier. See plan
 * 20260620-0741-archunit-enforce-dsl-driver-rules-investigation.md.
 */
@Tag("architecture")
class ArchitectureRulesTest {

    private static final JavaClasses TESTKIT = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.mycompany.myshop.testkit");

    /**
     * A1 — request DTOs in the driver port expose only String fields, or a nested value DTO from the driver port
     * (e.g. a shipping address) whose own fields are all String.
     */
    @Test
    void requestDtosDeclareOnlyStringFields() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat(
                        resideInAPackage("..driver.port.dtos..").and(simpleNameEndingWith("Request")))
                .and().areNotStatic()
                .should(beStringOrStringOnlyPortDto())
                .as("Request DTOs in driver.port.dtos must declare only String fields (or String-only nested port DTOs)");

        rule.check(TESTKIT);
    }

    /** A2 — verification public methods are fluent (own type) or terminal (void); never getters. */
    @Test
    void verificationMethodsAreFluentOrVoid() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat(
                        resideInAPackage("..dsl.core..").and(simpleNameEndingWith("Verification")))
                .and().arePublic()
                .should(returnOwningClassOrVoid())
                .as("Verification public methods must return their own type (fluent) or void (terminal) — no getters");

        rule.check(TESTKIT);
    }

    /** A7 — DSL core declares no own *Request/*Response; it reuses driver.port.dtos ("identical req/resp"). */
    @Test
    void dslCoreDeclaresNoOwnRequestOrResponseDtos() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..dsl.core..")
                .should().haveSimpleNameEndingWith("Request")
                .orShould().haveSimpleNameEndingWith("Response")
                .as("DSL core must not declare its own *Request/*Response DTOs — it shares driver.port.dtos");

        rule.check(TESTKIT);
    }

    /** A10 — every MyShopDriver operation takes a *Request and returns Result<*Response, …> (strict, Q2(b)). */
    @Test
    void everyDriverMethodTakesRequestAndReturnsResponse() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveSimpleName("MyShopDriver")
                .should(takeRequestAndReturnResponse())
                .as("Every MyShopDriver method must take a single *Request and return Result<*Response, …>");

        rule.check(TESTKIT);
    }

    private static ArchCondition<JavaField> beStringOrStringOnlyPortDto() {
        return new ArchCondition<>("have raw type String, or a driver.port.dtos type with only String fields") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                var type = field.getRawType();
                boolean ok = type.isEquivalentTo(String.class)
                        || (resideInAPackage("..driver.port.dtos..").test(type)
                            && type.getFields().stream()
                                .filter(nested -> !nested.getModifiers().contains(JavaModifier.STATIC))
                                .allMatch(nested -> nested.getRawType().isEquivalentTo(String.class)));
                if (!ok) {
                    events.add(SimpleConditionEvent.violated(field,
                            field.getFullName() + " has type " + type.getName()
                                    + " (expected String or a String-only driver.port.dtos type)"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> returnOwningClassOrVoid() {
        return new ArchCondition<>("return their declaring type (fluent) or void (terminal)") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                String returnType = method.getRawReturnType().getName();
                String owner = method.getOwner().getName();
                boolean ok = returnType.equals("void") || returnType.equals(owner);
                if (!ok) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " returns " + returnType + " (expected " + owner + " or void)"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> takeRequestAndReturnResponse() {
        return new ArchCondition<>("take a single *Request and return Result<*Response, …>") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                var params = method.getRawParameterTypes();
                if (params.size() != 1 || !params.get(0).getSimpleName().endsWith("Request")) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " must take exactly one *Request parameter (was " + params + ")"));
                }

                boolean responseOk = false;
                JavaType returnType = method.getReturnType();
                if (returnType instanceof JavaParameterizedType parameterized
                        && !parameterized.getActualTypeArguments().isEmpty()) {
                    String firstArg = parameterized.getActualTypeArguments().get(0).toErasure().getSimpleName();
                    responseOk = firstArg.endsWith("Response");
                }
                if (!responseOk) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " must return Result<*Response, …> (was " + returnType.getName() + ")"));
                }
            }
        };
    }
}
