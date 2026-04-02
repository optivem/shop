using Microsoft.EntityFrameworkCore;
using Optivem.Starter.Monolith.Api.Exception;
using Optivem.Starter.Monolith.Core.Services;
using Optivem.Starter.Monolith.Core.Services.External;
using Optivem.Starter.Monolith.Data;

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

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error");
}

app.UseStaticFiles();

app.UseRouting();

app.UseAuthorization();

app.MapRazorPages();
app.MapControllers();

app.Run();
