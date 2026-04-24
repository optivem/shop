# System configuration for the multitier architecture.
# Loaded (dot-sourced) by the parent Run-SystemTests.ps1 based on $Architecture.
# Sets $SystemConfig — a hashtable keyed by ExternalMode (real | stub).

$SystemConfig = @{
    "real" = @{
        ContainerName = "shop-real"

        SystemComponents = @(
            @{ Name = "Frontend";
                Url = "http://localhost:3211";
                ContainerName = "frontend" }
            @{ Name = "Backend API";
                Url = "http://localhost:8211/health";
                ContainerName = "backend" }
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
        ContainerName = "shop-stub"

        SystemComponents = @(
            @{ Name = "Frontend";
                Url = "http://localhost:3212";
                ContainerName = "frontend" }
            @{ Name = "Backend API";
                Url = "http://localhost:8212/health";
                ContainerName = "backend" }
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
