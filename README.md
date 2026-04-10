# HC_Professions_Services

A services board system for profession-based player services in Hytale. Players can create service listings advertising their profession skills, browse available services, and establish contracts with service providers through an interactive UI.

## Features

- Service listing creation with profession, title, description, and price fields
- Browsable services board UI page with listing details
- Contract system for establishing agreements between service providers and customers
- In-world Services Board block interaction to open the board UI
- PostgreSQL database storage for listings and contracts with HikariCP connection pooling
- Automatic expiry of old listings (7-day retention)
- Notification system for contract-related events with queued delivery on player connect
- Player command (`/services`) for accessing the services board
- Includes asset pack with UI files

## Dependencies

- Hytale:EntityModule

### Optional

- HC_Professions (profession data integration)

## Building

```bash
./gradlew build
```
