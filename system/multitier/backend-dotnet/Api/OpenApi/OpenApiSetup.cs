using Microsoft.OpenApi.Models;

namespace MyCompany.MyShop.Backend.Api.OpenApi;

/// <summary>
/// Code-first OpenAPI, the .NET twin of the Java backends' springdoc setup (OpenApiConfig): the document is generated
/// at runtime from the controllers and served at the same URL as springdoc's, <c>/v3/api-docs</c>, with Swagger UI at
/// <c>/swagger-ui</c>. Like the Java side, the document is not public: reading it needs an authenticated caller.
/// </summary>
public static class OpenApiSetup
{
    // Swashbuckle needs a document name in the route; naming the document "v3" puts it at springdoc's /v3/api-docs.
    private const string DocumentName = "v3";
    private const string DocumentRoute = "/{documentName}/api-docs";

    public static IServiceCollection AddShopOpenApi(this IServiceCollection services)
    {
        services.AddEndpointsApiExplorer();
        services.AddSwaggerGen(options =>
        {
            options.SwaggerDoc(DocumentName, new OpenApiInfo
            {
                Title = "MyShop API",
                Description = "REST API for the MyShop backend",
                Version = "1.0.0",
            });
            // Two DTOs with the same class name in different namespaces must not collide in the schema list.
            options.CustomSchemaIds(type => type.FullName?.Replace('+', '.'));
        });
        return services;
    }

    public static WebApplication MapShopOpenApi(this WebApplication app)
    {
        app.MapSwagger(DocumentRoute).RequireAuthorization();
        app.UseSwaggerUI(options =>
        {
            options.SwaggerEndpoint($"/{DocumentName}/api-docs", "MyShop API");
            options.RoutePrefix = "swagger-ui";
        });
        return app;
    }
}
