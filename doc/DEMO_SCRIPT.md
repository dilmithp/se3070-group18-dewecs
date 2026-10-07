# DEWECS viva demo script

Every step below was run against the `local` profile (in-memory H2 plus `DemoDataLoader`), and the expected
result is what the app actually returned. Steps that change data cannot be repeated without restarting.

## Start

```powershell
cd backend
mvn -B -DskipTests package
java -jar target\dewecs-0.0.1-SNAPSHOT.war --spring.profiles.active=local
```

Open http://localhost:8080. No environment variables are needed and the shared Neon database is never touched.

## Seeded records (IDs as shown in the URLs)

| Screen | Records |
|---|---|
| Warnings | 1 DRAFT (Galle flood), 2 ISSUED (Kandy landslide, expires tomorrow), 3 ISSUED but already past its expiry time |
| Shelters | 1 Galle Central College 9/10, 2 Kandy Town Hall 5/5 (FULL), 3 Colombo Sports Complex 0/150 |
| Rescue requests | 1 CRITICAL Galle pending, 2 HIGH Colombo pending, 3 assigned (Charlie team), 4 completed |
| Rescue teams | 1 Alpha (available), 2 Bravo (available), 3 Charlie (dispatched) |
| Relief supplies | 1 Rice 120 bags, 2 Bottled water 300 cases, 3 First-aid kits **6** (low stock, threshold 10), 4 Tarpaulins 45 |
| Distributions | 1 dispatched, 2 delivered, 3 cancelled |
| Ground reports | 1 and 2 PENDING_REVIEW, 3 VERIFIED, 4 PENDING_SYNC |
| Users | 1 Nadeesha Fernando (DMC officer), use her as the reviewing user |

## Click path

1. **Dashboard** (`/dashboard`). The five counts read **Open warnings 2, Full shelters 1, Pending rescue requests 2,
   Low-stock supplies 1, Unreviewed reports 2**. The open-warnings figure includes warning 3, which is overdue but not
   yet marked expired.
2. **Warnings** (`/warnings`). Warning 3 now shows **EXPIRED**: the list expires overdue issued warnings when it is
   opened. Go back to the dashboard: Open warnings is now **1**.
3. **Publish a draft.** Open warning 1, click Publish. Flash: *Warning published.* Status ISSUED; dashboard Open
   warnings is **2**.
4. **Retract it.** Click Retract. Flash: *Warning retracted.* Publishing it again shows *Only draft warnings can be
   edited or published.*
5. **Shelters** (`/shelters/1`). Check in one occupant (any name and NIC). Occupancy becomes 10/10, status **FULL**,
   dashboard Full shelters **2**. Check in another: *Cannot check in: shelter is at full capacity.*
6. **Close and reopen.** Close shelter 1: *Shelter closed.* Check in while closed: *Cannot check in: shelter is
   closed.* Reopen: *Shelter reopened.*
7. **Rescue requests** (`/rescue-requests/1`). Assign team 1 (Alpha): *Rescue team assigned.* Dashboard Pending
   rescue requests drops to **1**. Try to assign Alpha to request 2: *Team is not available for assignment.*
8. **Complete or cancel.** On request 1 click Complete: *Rescue request completed.* (Alpha becomes available
   again.) Cancelling an assigned request also frees the team: *Rescue request cancelled.*
9. **Relief supplies** (`/relief-supplies/3`). Restock First-aid kits by 10. Dashboard Low-stock supplies drops to **0**.
10. **Distributions** (`/relief-distributions/new`). Rice, Colombo Sports Complex, quantity 500: form re-shown with
    *Requested quantity exceeds available stock (120 bags available).* Quantity 20: created, supply 1 stock falls
    from 120 to **100**.
11. **Deliver, then try to cancel.** Click Deliver: *Distribution marked delivered.* Cancel is now refused:
    *Only dispatched (not yet delivered) distributions can be cancelled.* (Cancelling a dispatched one returns the
    stock; shown by `ReliefFlowTest`.)
12. **Ground reports** (`/ground-reports/1`). Review as Nadeesha: dashboard Unreviewed reports drops to **1**.
13. **Action needs a note.** On report 3 (already reviewed) click Action with an empty note: *An action note is
    required.* Enter *Team dispatched to Kolonnawa*: *Report actioned.* Status **ACTIONED**.
14. **Dismiss.** On report 2 click Dismiss: *Report dismissed.*
