using Microsoft.EntityFrameworkCore;
using Optivem.Starter.Backend.Api.Exception;
using Optivem.Starter.Backend.Core.Services;
using Optivem.Starter.Backend.Core.Services.External;
using Optivem.Starter.Backend.Data;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
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

// Configure CORS
var allowedOrigins = builder.Configuration.GetSection("Cors:AllowedOrigins").Get<string[]>() ?? [];

builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.WithOrigins(allowedOrigins)
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

// Configure PostgreSQL
var pgHost = Environment.GetEnvironmentVariable("POSTGRES_DB_HOST") ?? "localhost";
var pgPort = Environment.GetEnvironmentVariable("POSTGRES_DB_PORT") ?? "5432";
var pgName = Environment.GetEnvironmentVariable("POSTGRES_DB_NAME") ?? "starter";
var pgUser = Environment.GetEnvironmentVariable("POSTGRES_DB_USER") ?? "starter";
var pgPass = Environment.GetEnvironmentVariable("POSTGRES_DB_PASSWORD") ?? "starter";
var connectionString = $"Host={pgHost};Port={pgPort};Database={pgName};Username={pgUser};Password={pgPass}";
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(connectionString));

// Register services
builder.Services.AddScoped<ErpGateway>();
builder.Services.AddScoped<ClockGateway>();
builder.Services.AddScoped<OrderService>();

// Register exception handler
builder.Services.AddExceptionHandler<GlobalExceptionHandler>();
builder.Services.AddProblemDetails();

var app = builder.Build();

// Auto-migrate database
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    db.Database.EnsureCreated();
}

// Configure the HTTP request pipeline.
app.Use(async (context, next) =>
{
    context.Request.EnableBuffering();
    await next();
});

app.UseExceptionHandler();

app.UseRouting();

app.UseCors();

app.UseAuthorization();

app.MapControllers();

app.Run();
