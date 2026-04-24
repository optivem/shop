# System configuration for the multitier architecture.
# Loaded (dot-sourced) by the parent Run-SystemTests.ps1 based on $Architecture.
# Sets $SystemConfig — a hashtable keyed by ExternalMode (real | stub).

$SystemConfig = @{
    "real" = @{
        ContainerName = "shop-real"

        SystemComponents = @(
            @{ Name = "Frontend";
                Url = "http://localhost:3311";
                ContainerName = "frontend" }
            @{ Name = "Backend API";
                Url = "http://localhost:8311/health";
                ContainerName = "backend" }
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
            @{ Name = "Frontend";
                Url = "http://localhost:3312";
                ContainerName = "frontend" }
            @{ Name = "Backend API";
                Url = "http://localhost:8312/health";
                ContainerName = "backend" }
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
