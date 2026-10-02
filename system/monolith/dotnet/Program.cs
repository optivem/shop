using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authentication.OpenIdConnect;
using Microsoft.EntityFrameworkCore;
using MyCompany.MyShop.Monolith.Api.Exception;
using MyCompany.MyShop.Monolith.Api.Security;
using MyCompany.MyShop.Monolith.Core.Services;
using MyCompany.MyShop.Monolith.Core.Services.External;
using MyCompany.MyShop.Monolith.Data;

var builder = WebApplication.CreateBuilder(args);

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
var connectionString = $"Host={pgHost};Port={pgPort};Database={pgName};Username={pgUser};Password={pgPass}";
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(connectionString));

builder.Services.AddShopAuthentication();

// Register services
builder.Services.AddScoped<ErpGateway>();
builder.Services.AddScoped<ClockGateway>();
builder.Services.AddScoped<TaxGateway>();
builder.Services.AddScoped<CouponService>();
builder.Services.AddScoped<OrderService>();

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

app.UseAuthentication();
app.UseShopCsrfProtection();
app.UseAuthorization();

app.MapRazorPages();
app.MapControllers();

app.MapPost("/logout", async (HttpContext context) =>
{
    await context.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
    await context.SignOutAsync(OpenIdConnectDefaults.AuthenticationScheme,
        new AuthenticationProperties { RedirectUri = "/" });
});

await app.RunAsync();

public partial class Program { }
