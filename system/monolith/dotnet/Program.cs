using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authentication.OpenIdConnect;
using Microsoft.EntityFrameworkCore;
using MyCompany.MyShop.Monolith.Api.Exception;
using MyCompany.MyShop.Monolith.Api.Observability;
using MyCompany.MyShop.Monolith.Api.Resilience;
using MyCompany.MyShop.Monolith.Api.Security;
using MyCompany.MyShop.Monolith.Core.Services;
using MyCompany.MyShop.Monolith.Core.Services.External;
using MyCompany.MyShop.Monolith.Data;
using MyCompany.MyShop.Monolith.Pages.Storefront;

var builder = WebApplication.CreateBuilder(args);

// Structured JSON logs with trace IDs (always on); OpenTelemetry export only when OTEL_EXPORTER_OTLP_ENDPOINT is set.
builder.AddShopObservability();

// Add services to the container.
builder.Services.AddRazorPages();
builder.Services.AddControllers(options =>
{
    options.Filters.Add<ValidationProblemFilter>();
});
builder.Services.AddHttpClient();

// Configure JSON serialization
builder.Services.Configure<Microsoft.AspNetCore.Http.Json.JsonOptions>(options =>
{
    options.SerializerOptions.Converters.Add(new System.Text.Json.Serialization.JsonStringEnumConverter());
});
builder.Services.Configure<Microsoft.AspNetCore.Mvc.JsonOptions>(options =>
{
    options.JsonSerializerOptions.Converters.Add(new System.Text.Json.Serialization.JsonStringEnumConverter());
});

// Suppress default model validation response
builder.Services.Configure<Microsoft.AspNetCore.Mvc.ApiBehaviorOptions>(options =>
{
    options.SuppressModelStateInvalidFilter = true;
});

// Configure PostgreSQL
var pgHost = Environment.GetEnvironmentVariable("POSTGRES_DB_HOST") ?? "localhost";
var pgPort = Environment.GetEnvironmentVariable("POSTGRES_DB_PORT") ?? "5432";
var pgName = Environment.GetEnvironmentVariable("POSTGRES_DB_NAME") ?? "app";
var pgUser = Environment.GetEnvironmentVariable("POSTGRES_DB_USER") ?? "app";
var pgPass = Environment.GetEnvironmentVariable("POSTGRES_DB_PASSWORD") ?? "app";
// Pool capped at 10, the default of HikariCP (Java) and node-postgres/TypeORM (TypeScript): every stack shares
// one Neon compute (max_connections 112), and Npgsql's default of 100 per instance exhausted it in the cloud
// acceptance stage (Postgres 53300 -> HTTP 500).
var connectionString = $"Host={pgHost};Port={pgPort};Database={pgName};Username={pgUser};Password={pgPass};Maximum Pool Size=10";
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(connectionString));

builder.Services.AddShopAuthentication();

// Register services
// Typed HttpClients for the external systems, each behind timeout + retry + circuit breaker.
builder.Services.AddShopExternalSystemClients(builder.Configuration);
builder.Services.AddScoped<CouponService>();
builder.Services.AddScoped<OrderService>();
builder.Services.AddScoped<ProfileService>();
builder.Services.AddScoped<ProductService>();

// The storefront (storefront-ux §6.3): the cart lives in the server session, whose ID travels only in an
// HttpOnly cookie (never in page links). The session cookie is independent of the sign-in cookie, so the cart
// survives sign-in.
builder.Services.AddDistributedMemoryCache();
builder.Services.AddSession(options =>
{
    options.Cookie.Name = "myshop.session";
    options.Cookie.HttpOnly = true;
    options.Cookie.IsEssential = true;
    options.Cookie.SameSite = SameSiteMode.Lax;
    options.Cookie.SecurePolicy = CookieSecurePolicy.SameAsRequest;
    options.IdleTimeout = TimeSpan.FromMinutes(30);
});
builder.Services.AddHttpContextAccessor();
builder.Services.AddScoped<CatalogCache>();
builder.Services.AddScoped<StoreChrome>();
builder.Services.AddScoped<CheckoutPlacer>();

// Register exception handler
builder.Services.AddExceptionHandler<GlobalExceptionHandler>();
builder.Services.AddProblemDetails();

var app = builder.Build();

// Configure the HTTP request pipeline.
app.Use(async (context, next) =>
{
    context.Request.EnableBuffering();
    await next();
});

app.UseExceptionHandler();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error");
}

app.UseStaticFiles();

app.UseRouting();

app.UseSession();

app.UseAuthentication();
app.UseShopCsrfProtection();
app.UseAuthorization();

app.MapRazorPages();
app.MapControllers();

// The header's "Sign in" link (storefront-ux §6.4). The route requires a login, so an anonymous visitor is sent to
// Keycloak and, once signed in, comes back here and on to returnUrl (default "/"); a signed-in visitor goes straight there.
app.MapGet("/sign-in", (string? returnUrl) => Results.Redirect(ReturnUrls.Safe(returnUrl)));

app.MapPost("/logout", async (HttpContext context) =>
{
    await context.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
    await context.SignOutAsync(OpenIdConnectDefaults.AuthenticationScheme,
        new AuthenticationProperties { RedirectUri = "/" });
});

await app.RunAsync();

public partial class Program { }
