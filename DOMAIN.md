# Domain Model

Reference for the business logic of the app: what each entity is, how they relate,
and why. Programming concepts live in [GLOSSARY.md](GLOSSARY.md).

Legend: ✅ built · ⏳ planned · ❓ open question

---

## The core idea

Every expense follows the same shape:

> A **Bill** (one expense) is split by a **SplitRule** among the participating
> **Apartments**, producing one **Debt** per apartment. Apartments settle their
> total balance with **Payments**.

```
Category 1 ──── N Bill N ──── 1 SplitRule (enum)
                   │
                   │ 1
                   │
                   N
                 Debt N ──── 1 Apartment 1 ──── N Payment
                                  │ 1
                                  │
                                  0..1
                               AppUser
```

---

## Entities

### ✅ Apartment — table `apartment`
The fixed apartments of the building (Ισόγειο, 1ος, 2ος). Seeded on startup.

| Field | Type | Constraints |
|---|---|---|
| id | Long | PK, IDENTITY |
| name | String | not null, unique |

### ✅ AppUser — table `app_user`
A login account: the admin plus one resident per apartment.

| Field | Type | Constraints |
|---|---|---|
| id | Long | PK, IDENTITY |
| firstName, lastName | String | not null |
| email | String | not null, unique (the login username) |
| password | String | not null — will hold a BCrypt **hash**, never plain text |
| role | Role (enum) | not null, `@Enumerated(STRING)` |
| apartment | Apartment | `@OneToOne`, FK `apartment_id`, **nullable** |

Why nullable: the admin's access comes from the role, not from an apartment.

### ✅ Category — table `category`
Groups bills: Κοινόχρηστο Ρεύμα, Φυσικό αέριο, Απολύμανση. Seeded on startup.
An **entity, not an enum**, because the admin can add categories at runtime.

| Field | Type | Constraints |
|---|---|---|
| id | Long | PK, IDENTITY |
| name | String | not null, unique |

### ✅ Bill — table `bill` (base class, `@Inheritance(JOINED)`)
One expense of one month. Holds the **data**; the SplitRule holds the **logic**.

| Field | Type | Constraints |
|---|---|---|
| id | Long | PK, IDENTITY |
| amount | BigDecimal(10,2) | not null — may be negative (credit bill) |
| billDate | LocalDate | not null, column `bill_date` |
| category | Category | `@ManyToOne`, FK `category_id`, not null |
| splitRule | SplitRule (enum) | not null, `@Enumerated(STRING)`, column `split_rule` |

Electricity bills, gas faults/maintenance and disinfection are plain `Bill`s
(only `amount` needed).

### ✅ GasBill extends Bill — table `gas_bill`
The monthly gas bill, which needs meter data. Shares the `id` with `bill`
(`gas_bill.id` is a FK to `bill.id`).

| Field | Type | Excel | Constraints |
|---|---|---|---|
| meterFloor | Integer | Μ1 — meter of the floor (1ος) | not null, column `meter_floor` |
| meterTotal | Integer | ΜΣ — total building meter | not null, column `meter_total` |
| fixedCharge | BigDecimal(10,2) | ΕΔΑ+ΔΕΣΦΑ — fixed part | not null, column `fixed_charge` |

**Ω** (variable, consumption-based cost) = `amount − fixedCharge`.
Computed, **not stored**.

### ✅ Debt — table `debt`
What **one** apartment owes for **one** bill. Permanent record, never deleted.

| Field | Type | Constraints |
|---|---|---|
| id | Long | PK, IDENTITY |
| amount | BigDecimal(10,2) | not null — may be negative |
| bill | Bill | `@ManyToOne`, FK `bill_id`, not null |
| apartment | Apartment | `@ManyToOne`, FK `apartment_id`, not null |

No paid/unpaid status.

### ✅ Payment — table `payment`
Money an apartment handed over, as a lump sum covering many debts.

| Field | Type | Constraints |
|---|---|---|
| id | Long | PK, IDENTITY |
| amount | BigDecimal(10,2) | not null |
| paymentDate | LocalDate | not null, column `payment_date` |
| apartment | Apartment | `@ManyToOne`, FK `apartment_id`, not null |

Linked to the **Apartment**, not to specific Debts.

---

## Relationships

| Relationship | Cardinality | Annotation (owning side) | FK lives in |
|---|---|---|---|
| AppUser → Apartment | 1 – 0..1 | `@OneToOne` on AppUser | `app_user.apartment_id` |
| Bill → Category | N – 1 | `@ManyToOne` on Bill | `bill.category_id` |
| GasBill → Bill | inheritance | `@Inheritance(JOINED)` on Bill | `gas_bill.id` |
| Debt → Bill | N – 1 | `@ManyToOne` on Debt | `debt.bill_id` |
| Debt → Apartment | N – 1 | `@ManyToOne` on Debt | `debt.apartment_id` |
| Payment → Apartment | N – 1 | `@ManyToOne` on Payment | `payment.apartment_id` |

All relationships are **unidirectional** for now (the "many" side knows the "one"
side, not the other way round).

---

## Enums

| Enum | Values | Why enum |
|---|---|---|
| Role | ADMIN, RESIDENT | fixed, developer-defined |
| SplitRule | EQUAL_SPLIT, METER_BASED | fixed, developer-defined; the admin chooses, never creates |

---

## Split rules

**EQUAL_SPLIT** — `amount / number of participating apartments`.
Electricity, gas faults, gas maintenance (÷2); disinfection (÷3).

**METER_BASED** — monthly gas bill only:
- Ω = amount − fixedCharge
- 1ος = Ω × (meterFloor / meterTotal) + fixedCharge / 2
- Ισόγειο = amount − 1ος

---

## Locked principles

1. **1 Bill → N Debts** — one debt per participating apartment.
2. **The rule lives on the Bill**, not the Category (gas has two rules).
3. **Logic (SplitRule) ≠ data (Bill)** — the rule is shared and stateless; monthly
   values live on the bill.
4. **Inheritance on Bill (JOINED)** — no nullable meter columns on non-gas bills.
5. **Overpayment (ρέστα) = negative balance**, not a separate field.
6. **No stored totals** — balance = Σ(debts) − Σ(payments). One source of truth.
7. **Negative amounts self-balance** — no special handling.
8. **Debt has no paid/unpaid status** — payments settle the balance in aggregate,
   not debt-by-debt.
9. **Payment links to Apartment, not to Debts** — the owner is paid a lump sum, so
   a payment is never allocated against specific debts.

---

## Open questions

- ❓ **Participants:** where do we store *which* apartments share an expense
  (electricity: Ισόγειο + 1ος; disinfection: all three)? Candidates: on the
  Category (`@ManyToMany` Category ↔ Apartment) or chosen per Bill.
  Must be decided before the calculation service.
- ❓ **Where the split logic lives:** methods on the `SplitRule` enum, or a
  calculation service that switches on the rule.
- ❓ **Export format** (Excel or PDF) — after the core works.
