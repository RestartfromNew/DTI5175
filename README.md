```mermaid
flowchart TD
    A[Launch App]
    B{Is First Launch?}
    C[Login]
    D[Onboarding page]
    E[main page]

    A --> B
    B -->|Yes| D --> C --> E
    B -->|No| C --> E
```
