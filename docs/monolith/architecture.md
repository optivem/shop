# Shop — System Architecture

```mermaid
graph LR
    User -->|UI / API| Shop[Shop]
    Shop -->|REST| ERP
    Shop -->|REST| Clock
```
