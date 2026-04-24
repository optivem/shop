# System configuration for the monolith architecture.
# Loaded (dot-sourced) by the parent Run-SystemTests.ps1 based on $Architecture.
# Sets $SystemConfig — a hashtable keyed by ExternalMode (real | stub).

$SystemConfig = @{
    "real" = @{
        ContainerName = "shop-real"

        SystemComponents = @(
            @{ Name = "Monolith";
                Url = "http://localhost:3311";
                ContainerName = "sysapp" }
            @{ Name = "Monolith API";
                Url = "http://localhost:8311/health";
                ContainerName = "sysapp" }
        )

        ExternalSystems = @(
            @{ Name = "ERP API (Real)";
                Url = "http://localhost:9311/erp/health";
                ContainerName = "external-real" }
            @{ Name = "Clock API (Real)";
                Url = "http://localhost:9311/clock/health";
                ContainerName = "external-real" }
        )
    }

    "stub" = @{
        ContainerName = "shop-stub"

        SystemComponents = @(
            @{ Name = "Monolith";
                Url = "http://localhost:3312";
                ContainerName = "sysapp" }
            @{ Name = "Monolith API";
                Url = "http://localhost:8312/health";
                ContainerName = "sysapp" }
        )

        ExternalSystems = @(
            @{ Name = "ERP API (Stub)";
                Url = "http://localhost:9312/erp/health";
                ContainerName = "external-stub" }
            @{ Name = "Clock API (Stub)";
                Url = "http://localhost:9312/clock/health";
                ContainerName = "external-stub" }
        )
    }
}

return $SystemConfig
