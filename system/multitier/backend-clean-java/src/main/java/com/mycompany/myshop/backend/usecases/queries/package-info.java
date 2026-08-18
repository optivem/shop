/**
 * The read side. Ports here answer a question with the columns the database already holds, and the
 * records they return are projections: flat, primitive, and built by nobody but the adapter.
 *
 * <p>Deliberately <em>not</em> {@code domain.repositories}. A port in the domain claims the domain
 * needs it, and the domain never calls any of these. Placement is not what bypasses the domain
 * model — the projection return type is; placement is what makes the code admit it.
 *
 * <p>What is given up: the read model can now drift from the domain's idea of an order or a coupon,
 * because nothing forces the two to agree. Speed and failure-isolation are bought by surrendering
 * the guarantee that what is displayed was validated by the rules that wrote it.
 */
package com.mycompany.myshop.backend.usecases.queries;
