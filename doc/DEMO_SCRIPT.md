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

Statuses, severities and priorities appear as coloured badges with readable text (the stored value ISSUED shows
as "Issued", PENDING_REVIEW as "Pending review"). Destructive buttons (Retract, Close, Cancel, Dismiss) ask for
confirmation in the browser first.

1. **Dashboard** (`/dashboard`). The five counts read **Open warnings 2, Full shelters 1, Pending rescue requests 2,
   Low-stock supplies 1, Unreviewed reports 2**. The open-warnings figure includes warning 3, which is overdue but not
   yet marked expired.
2. **Warnings** (`/warnings`). Warning 3 now shows the **Expired** badge: the list expires overdue issued warnings when it is
   opened. Go back to the dashboard: Open warnings is now **1**.
3. **Publish a draft.** Open warning 1, click Publish. Flash: *Warning published.* Status badge **Issued**; dashboard Open
   warnings is **2**.
4. **Retract it.** Click Retract. Flash: *Warning retracted.* Publishing it again shows *Only draft warnings can be
   edited or published.*
5. **Shelters** (`/shelters/1`). Check in one occupant (any name and NIC). Occupancy becomes 10/10 (the bar turns red), status badge **Full**,
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
    required.* Enter *Team dispatched to Kolonnawa*: *Report actioned.* Status badge **Actioned**.
14. **Dismiss.** On report 2 click Dismiss: *Report dismissed.*

## UC-01 walk-through: hazard event, evidence, basin warning, broadcast, escalation

The local demo data has four hazard events (1 flood Galle, 2 landslide Kandy, 3 cyclone Colombo, 4 flood Colombo), a
fourth district (Gampaha), three river basins (Kelani Ganga runs through Colombo and Gampaha) and warning 4, a DRAFT
Kelani flood notice for both districts on SMS and app push. Warnings 2 and 3 (already issued) have a delivery log.
These steps are covered by `Uc01FlowTest`, `Uc01ChannelFailureFlowTest` and `DemoDataLoaderTest`; they were not
click-tested one by one, so check the numbers on screen.

0. **One-screen issue page** (`/warnings/issue`, button *+ Issue warning* on the warnings list). It follows Group 20's wireframe: hazard type, severity, *By District* or *By River Basin*, message, optional title and *Issue Warning* on the left; the map and the *Active Warnings* list with a one-click *Escalate* button on the right. Issuing registers the hazard event, drafts and publishes the warning and broadcasts it in one step.
1. **Dashboard.** Under the five counts, *Active hazard events* lists the four events with their verified field reports
   and live warnings, and Review and Issue warning links.
2. **Hazard events** (`/hazard-events`). *Register hazard event*: choose Flood, High and a district (leave the start time
   empty). The event starts **Active**. A start time in the future is refused.
3. **Review the evidence.** Open event 4. Only *verified* ground reports of that district show (the demo has one in
   Colombo). Unreviewed reports never appear, so an unverified report cannot influence a warning.
4. **Target a river basin.** Open warning 4: *Regions covered* is Colombo, Gampaha. When drafting a new warning, the
   *Target a river basin* list adds every district of the basin to the event district.
5. **Publish and broadcast.** Publish warning 4. The page now shows a **Delivery log**: one row per channel (SMS, app push),
   result *Sent*, and how many registered citizens were addressed. The dashboard Open warnings goes from 2 to **3**.
6. **Map.** On `/warnings`, *Active warnings map* marks Kandy, Colombo and Gampaha, coloured by the highest severity. A
   marker (or its name in the list under the map) filters the table to that district.
7. **Escalate.** On warning 4 use *Escalate this warning*: pick Critical, add a reason, submit. Flash: *Warning escalated to
   CRITICAL and sent again.* Status **Updated**, the issue time is unchanged, the reason is added to the message, the log
   gets new rows marked *Escalation*, and the warning still counts as open. Escalating to the same or a lower severity is
   refused. *Escalate* is also a link on every live row of the warnings list.
8. **Channel failure.** Restart the app with `--dewecs.alerts.simulate-failures=SMS` (the simulated SMS gateway then always
   fails) and publish a warning with SMS: the log shows two failed SMS attempts, then radio *Sent (instead of sms)*. Use
   `SIREN` to see a channel with no fallback stay undelivered: the page warns, and *Send again on the failed channels*
   tries again and logs the retry.
9. **JSON.** Every page above also answers `Accept: application/json`; the new actions take JSON bodies (see the
   Postman folder *Officer JSON examples*).

## UC-02 walk-through: relief command center and analytics report

1. Relief Center: pick an incident, review inventory; record a shelter need; press Allocate on it.
2. Dispatch more than 85% of one stock: read the split proposal, Accept split and dispatch.
3. Open a consignment: re-route it, then Confirm field handover with a damaged quantity.
4. Agency notifications: see what each agency was told.
5. Post-Event Analysis: generate with an open consignment, pick Provisional draft, view indicators, download PDF and CSV.

## UC-03 walk-through: coordinate shelter and rescue

1. Coordination: pick a district; read the shelters, teams and incidents.
2. Set a shelter occupancy above capacity (rejected, alternates suggested), then to the capacity (full flag).
3. Dispatch a team to a pending incident: filter by capability, tick the handshake for another organization's team.
4. Report the team En route, On site, Needs support (backups listed), Task complete.
5. Read the audit trail.
