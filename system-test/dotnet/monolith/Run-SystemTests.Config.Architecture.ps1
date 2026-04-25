# System configuration for the monolith architecture.
# Loaded (dot-sourced) by the parent Run-SystemTests.ps1 based on $Architecture.
# Sets $SystemConfig — a hashtable keyed by ExternalMode (real | stub).

$SystemConfig = @{
    "real" = @{
        ContainerName = "my-shop-real"

        SystemComponents = @(
            @{ Name = "Monolith";
                Url = "http://localhost:3211";
                ContainerName = "system" }
            @{ Name = "Monolith API";
                Url = "http://localhost:8211/health";
                ContainerName = "system" }
        )

        ExternalSystems = @(
            @{ Name = "ERP API (Real)";
                Url = "http://localhost:9211/erp/health";
                ContainerName = "external-real" }
            @{ Name = "Clock API (Real)";
                Url = "http://localhost:9211/clock/health";
                ContainerName = "external-real" }
        )
    }

    "stub" = @{
        ContainerName = "my-shop-stub"

        SystemComponents = @(
            @{ Name = "Monolith";
                Url = "http://localhost:3212";
                ContainerName = "system" }
            @{ Name = "Monolith API";
                Url = "http://localhost:8212/health";
                ContainerName = "system" }
        )

        ExternalSystems = @(
            @{ Name = "ERP API (Stub)";
                Url = "http://localhost:9212/erp/health";
                ContainerName = "external-stub" }
            @{ Name = "Clock API (Stub)";
                Url = "http://localhost:9212/clock/health";
                ContainerName = "external-stub" }
        )
    }
}

return $SystemConfig
