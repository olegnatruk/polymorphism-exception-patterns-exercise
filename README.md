# Parcel Delivery System

A simple Java console application for the **polymorphism, exception handling, Singleton, and Strategy** exercise. Add Document, Box, or Fragile parcels; view their fees and status; change a pending parcel's delivery service; and mark parcels delivered.

## Requirements and running

Install **JDK 17 or newer**. No Maven, Gradle, database, or external libraries are required. Run these commands from the repository folder in a terminal:

```sh
mkdir -p out
javac --release 17 -d out src/*.java
java -cp out ParcelDeliverySystem
```

For Windows Command Prompt, use `mkdir out` instead of `mkdir -p out`. The Java commands are the same.

## Menu

```text
1. Add Parcel
2. View All Parcels
3. Change Delivery Service
4. Mark as Delivered
5. Exit
```

Enter a manually assigned tracking number matching uppercase `ABC` followed by four digits (for example, `ABC0001`). Tracking numbers must be unique in the current session. Text is trimmed; receiver names must be **3–20 characters** after trimming, and handling notes cannot be blank. Invalid input is explained and prompted again. New parcels start as Pending.

Parcels are kept **in memory only** and are discarded when the program exits. End of input exits cleanly without storing an unfinished parcel. Delivered parcels remain visible, and their service cannot be changed.

When no parcels have been added, View All Parcels, Change Delivery Service, and Mark as Delivered display `No parcels available.` and return to the menu without requesting a tracking number.

## Parcel and pricing rules

| Type | Additional details | Maximum actual weight | Chargeable weight | Base fee |
| --- | --- | --- | --- | --- |
| Document | 1–100 pages | 0.5 kg | Actual weight | Php 50 |
| Box | Positive integer length, width, height in cm | 50 kg | Ceiling of the larger of actual and volumetric weight | Php 40 × chargeable weight |
| Fragile | Box dimensions plus non-empty handling note | 30 kg | Same as Box | Box base fee + Php 100 |

Volumetric weight is `(length × width × height) / 3500`. Box and Fragile weights round upward: `2.0 → 2`, `2.1 → 3`. Actual weight limits do not cap volumetric weight.

| Service | Delivery fee | Restriction |
| --- | --- | --- |
| Standard | Base fee | None |
| Express | Base fee × 1.5 | None |
| Same Day | Base fee × 2 + Php 100 | Chargeable weight ≤ 10 kg |

Weights and money use `BigDecimal`. Volume multiplication avoids integer overflow, and chargeable weight is calculated without an approximate intermediate division. Fees are displayed with two decimal places.

An ineligible Same Day selection while adding a parcel repeats service selection. An ineligible service change shows `Same Day delivery is only for parcels up to 10 kg!` and returns to the main menu, keeping the previous service and fee.

Selecting a pending parcel's current service displays `It is already Standard`, `It is already Express`, or `It is already Same Day` and returns to the menu. The parcel keeps its existing service and fee.

## Required object-oriented concepts

- **Polymorphism:** abstract `Parcel` defines `getType()`, `getChargeableWeight()`, and `getBaseFee()`. `Document`, `Box`, and `Fragile` override behavior. The table iterates over `Parcel` references and calls those methods without selecting a fee formula by type. `Fragile extends Box` reuses the Box chargeable weight and adds its handling fee to `super.getBaseFee()`.
- **Strategy:** `DeliveryStrategy` defines pricing and eligibility. `StandardDelivery`, `ExpressDelivery`, and `SameDayDelivery` implement it. Each parcel delegates pricing to its selected strategy. `changeDeliveryService()` validates the replacement before assigning it.
- **Singleton:** `ParcelManager` has a private constructor, one static instance, and `getInstance()`. Its `LinkedHashMap<String, Parcel>` enforces uniqueness and preserves insertion order. The collection is exposed as an immutable list snapshot.
- **Exception handling:** custom checked exceptions extend `ParcelException`: `InvalidParcelException`, `DuplicateTrackingNumberException`, `DeliveryServiceUnavailableException`, `ParcelNotFoundException`, and `ParcelAlreadyDeliveredException`. The console catches recoverable errors and continues. Numeric parsing errors are caught separately, and end of input has a clean exit path.

## Example session

For a Fragile parcel, choose these inputs in order:

```text
Menu option:        1 (Add Parcel)
Parcel type:        3 (Fragile)
Tracking number:    ABC0001
Receiver name:      Ana Cruz
Weight:             1.8
Length:             40
Width:              20
Height:             10
Handling note:      Keep upright
Delivery service:   2 (Express)
```

The program reports:

```text
Parcel added successfully! Delivery fee: Php 330.00
```

Volume is `8000 cm³`, volumetric weight is `8000 / 3500 kg`, and chargeable weight is `3 kg`. Base fee is `(40 × 3) + 100 = Php 220`; Express fee is `220 × 1.5 = Php 330.00`.

Choose menu option `3`, enter `ABC0001`, and select `3` (Same Day):

```text
Delivery service changed! New fee: Php 540.00
```

Choose option `4` with `ABC0001`:

```text
Parcel delivered successfully!
```

Repeating delivery reports `Parcel was already delivered.` Changing its service reports `Parcel already delivered!`. Looking up an unknown valid tracking number reports `Parcel not found!`. Option `2` shows all parcels with tracking number, type, receiver, chargeable weight, service, fee, and status.

## Testing

```sh
mkdir -p out
javac --release 17 -d out src/*.java tests/*.java
java -cp out ParcelDeliverySystemTest
```

The dependency-free test runner exits with an error if a check fails. It checks field and weight boundaries, upward rounding and integer overflow, all three pricing strategies, Same Day eligibility using actual and volumetric weights, the assignment's Php 330.00 example, Singleton identity, duplicate protection, delivery transitions, immutable collection access, and scripted console flows. Console tests cover all five operations, input retries, exact required messages, rejected service changes, table contents, and end of input.

## Project layout

```text
src/      Application, parcel classes, strategies, manager, and exceptions
tests/    Dependency-free Java regression test runner
out/      Generated class files (ignored by Git)
```
