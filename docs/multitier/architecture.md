# Shop — System Architecture

```mermaid
graph LR
    User -->|UI| Frontend
    User -->|API| Backend
    Frontend -->|REST| Backend
    Backend -->|REST| ERP
    Backend -->|REST| Clock
```
