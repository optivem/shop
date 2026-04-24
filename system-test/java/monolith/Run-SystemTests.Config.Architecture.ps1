# System configuration for the monolith architecture.
# Loaded (dot-sourced) by the parent Run-SystemTests.ps1 based on $Architecture.
# Sets $SystemConfig — a hashtable keyed by ExternalMode (real | stub).

$SystemConfig = @{
    "real" = @{
        ContainerName = "shop-real"

        SystemComponents = @(
            @{ Name = "Monolith";
                Url = "http://localhost:3111";
                ContainerName = "monolith" }
            @{ Name = "Monolith API";
                Url = "http://localhost:8111/health";
                ContainerName = "monolith" }
        )

        ExternalSystems = @(
            @{ Name = "ERP API (Real)";
                Url = "http://localhost:9111/erp/health";
                ContainerName = "external-real" }
            @{ Name = "Clock API (Real)";
                Url = "http://localhost:9111/clock/health";
                ContainerName = "external-real" }
        )
    }

    "stub" = @{
        ContainerName = "shop-stub"

        SystemComponents = @(
            @{ Name = "Monolith";
                Url = "http://localhost:3112";
                ContainerName = "monolith" }
            @{ Name = "Monolith API";
                Url = "http://localhost:8112/health";
                ContainerName = "monolith" }
        )

        ExternalSystems = @(
            @{ Name = "ERP API (Stub)";
                Url = "http://localhost:9112/erp/health";
                ContainerName = "external-stub" }
            @{ Name = "Clock API (Stub)";
                Url = "http://localhost:9112/clock/health";
                ContainerName = "external-stub" }
        )
    }
}

return $SystemConfig
