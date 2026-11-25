package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A visitor's sign-up (UC-013): the new sign-in account and the shipping country chosen on the registration form. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String country;

    @Override
    public String toString() {
        return "SignUpRequest[username=" + username + ", email=" + email + ", country=" + country + "]";
    }
}
