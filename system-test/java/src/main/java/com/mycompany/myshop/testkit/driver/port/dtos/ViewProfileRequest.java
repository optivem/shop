package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewProfileRequest {
    /** {@link #whenAskedToSignIn}: a visitor who is asked to sign in signs in as the customer (UI only). */
    public static final String SIGN_IN = "sign-in";

    /** What a visitor does when the profile asks to sign in: {@link #SIGN_IN}, or nothing (unset). */
    private String whenAskedToSignIn;
}
