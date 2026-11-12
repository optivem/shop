package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import com.mycompany.myshop.testkit.driver.port.dtos.UpdateProfileRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileResponse;

/**
 * The profile `/account` (My account › Profile &amp; address, UC-010): the name, the email from the sign-in account
 * (read-only), the default shipping address and its country select, and a note on how tax is charged. Saving shows a
 * notification: "Your profile has been saved.", or the validation error as {@code field: message} lines.
 */
public class ProfilePage extends BasePage {
    private static final String SAVE_PROFILE_SELECTOR = "button[aria-label='Save Profile']";
    /** The profile has loaded (the single-page UIs read it first). */
    public static final String READY_SELECTOR = SAVE_PROFILE_SELECTOR;
    private static final String FIRST_NAME_SELECTOR = "input[aria-label='First Name']";
    private static final String LAST_NAME_SELECTOR = "input[aria-label='Last Name']";
    private static final String EMAIL_SELECTOR = "input[aria-label='Email']";
    private static final String STREET_SELECTOR = "input[aria-label='Street']";
    private static final String POSTAL_CODE_SELECTOR = "input[aria-label='Postal Code']";
    private static final String CITY_SELECTOR = "input[aria-label='City']";
    private static final String COUNTRY_SELECTOR = "select[aria-label='Profile Country']";
    private static final String COUNTRY_OPTION_SELECTOR = COUNTRY_SELECTOR + " option";
    private static final String TAX_NOTICE_SELECTOR = "[aria-label='Tax Country Hint']";

    public ProfilePage(PageClient pageClient) {
        super(pageClient);
    }

    public void waitForLoaded() {
        pageClient.waitForVisible(READY_SELECTOR);
    }

    /** Enters every field; a country the select does not offer is added to it, as an edited form would send it. */
    public void input(UpdateProfileRequest request) {
        pageClient.fill(FIRST_NAME_SELECTOR, request.getFirstName());
        pageClient.fill(LAST_NAME_SELECTOR, request.getLastName());
        pageClient.fill(STREET_SELECTOR, request.getStreet());
        pageClient.fill(POSTAL_CODE_SELECTOR, request.getPostalCode());
        pageClient.fill(CITY_SELECTOR, request.getCity());
        pageClient.selectOptionByValueAddingIfMissing(COUNTRY_SELECTOR, request.getCountry() == null ? "" : request.getCountry());
    }

    public void clickSaveProfile() {
        pageClient.click(SAVE_PROFILE_SELECTOR);
    }

    /** The profile as the page shows it now; an empty field reads as "". */
    public ViewProfileResponse read() {
        waitForLoaded();
        var emailReadOnly = pageClient.readAttribute(EMAIL_SELECTOR, "readonly") != null
                || pageClient.readAttribute(EMAIL_SELECTOR, "disabled") != null;
        var countryChoices = pageClient.readAllAttributesNoWait(COUNTRY_OPTION_SELECTOR, "value").stream()
                .filter(value -> value != null && !value.isEmpty())
                .toList();
        return ViewProfileResponse.builder()
                .firstName(pageClient.readInputValue(FIRST_NAME_SELECTOR))
                .lastName(pageClient.readInputValue(LAST_NAME_SELECTOR))
                .email(pageClient.readInputValue(EMAIL_SELECTOR))
                .street(pageClient.readInputValue(STREET_SELECTOR))
                .postalCode(pageClient.readInputValue(POSTAL_CODE_SELECTOR))
                .city(pageClient.readInputValue(CITY_SELECTOR))
                .country(pageClient.readInputValue(COUNTRY_SELECTOR))
                .emailChangeable(!emailReadOnly)
                .countryChoices(countryChoices)
                .taxNotice(pageClient.readTextContent(TAX_NOTICE_SELECTOR).trim().replaceAll("\\s+", " "))
                .build();
    }
}
