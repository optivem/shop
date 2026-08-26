/**
 * The read side, grouped by subject: {@code order}, {@code coupon} and {@code report} each hold the
 * whole of one read -- the use cases, their requests and responses, and a {@code ports} subpackage
 * holding the one {@code *Reader} the subject is asked through. The port sits in its own package so
 * that opening a subject shows the reads first rather than sorting an interface in among them.
 *
 * <p>The ports are {@code *Reader} rather than {@code *Query} so that the CQRS split reads as a
 * pair: {@code OrderRepository} writes and hands back an aggregate, {@code OrderReader} reads and
 * hands back a projection. The side is named {@code queries} and the port is named for its role,
 * exactly as {@code usecases.commands} holds ports called {@code *Repository} and {@code *Gateway}
 * rather than {@code *Command}.
 * {@code common} holds the two types that belong to no use case, {@code Page} and {@code PageSpec}.
 *
 * <p>A port here answers a question with the columns the database already holds, and what it
 * returns is a projection: flat, primitive, and built by nobody but the adapter.
 *
 * <p>The projection <em>is</em> the response, and is named as one. {@code OrderReader} returns
 * {@code ViewOrderDetailsResponse} rather than an {@code OrderDetail} that a use case would then
 * copy field by field into a response of the same fifteen names. A query reports stored columns, so
 * anything sitting between the port and the wire could only ever be that transcription. Where a
 * response has parts, the parts take the use case's prefix too --
 * {@code BrowseOrderHistoryItemResponse} is the row inside {@code BrowseOrderHistoryResponse}.
 *
 * <p>Nothing in this package tree may import {@code ..domain..}, and
 * {@code ArchitectureTest.READ_USECASES_DO_NOT_TOUCH_THE_DOMAIN} is that sentence made executable.
 * The split from {@code usecases.commands} is what lets the rule name a package instead of listing
 * classes: a new query is covered by where it is filed, not by someone remembering to add it.
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
