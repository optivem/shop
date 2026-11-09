package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.UpdateProfileRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;

/** Saves the acting customer's profile. Values are literals: the profile accepts only US, DE or GB. */
public class UpdateProfile extends BaseMyShopUseCase<ViewProfileResponse, ViewProfileVerification> {
    private String firstName;
    private String lastName;
    private String street;
    private String postalCode;
    private String city;
    private String country;

    public UpdateProfile(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public UpdateProfile firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UpdateProfile lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UpdateProfile street(String street) {
        this.street = street;
        return this;
    }

    public UpdateProfile postalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public UpdateProfile city(String city) {
        this.city = city;
        return this;
    }

    public UpdateProfile country(String country) {
        this.country = country;
        return this;
    }

    @Override
    public UseCaseResult<ViewProfileResponse, ViewProfileVerification> execute() {
        var request = UpdateProfileRequest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .street(street)
                .postalCode(postalCode)
                .city(city)
                .country(country)
                .build();
        var result = driver.updateProfile(request);
        return new UseCaseResult<>(result, context, ViewProfileVerification::new);
    }
}
