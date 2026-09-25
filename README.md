# Real-Estate Property Management System

A Java Swing desktop application for managing residential properties, backed by a MySQL database.

It records:

- **The estate:** properties, their buildings, the floors in each building and the units on each floor.
- **Tenancy:** tenants, the leases that connect them to units (overlapping leases on a unit are refused) and rent payments.
- **Costs:** expenses charged to a whole property or to a single unit.
- **Maintenance:** requests for a property or a unit, broken into tasks and subtasks nested to any depth.
  The Maintenance screen can total the cost and progress of any task and everything beneath it.

The app has ten screens, reached from the sidebar: Properties, Units, Buildings, Floors, Hierarchy,
Maintenance, Tenants, Leases, Payments and Expenses.

---

## Contents

1. [Quick start: run the packaged app](#1-quick-start-run-the-packaged-app)
2. [Database setup](#2-database-setup)
3. [Build from source](#3-build-from-source)
4. [Build the Windows package yourself](#4-build-the-windows-package-yourself)
5. [Tests and test report](#5-tests-and-test-report)
6. [Demo data](#6-demo-data)
7. [Project structure](#7-project-structure)
8. [Troubleshooting](#8-troubleshooting)
9. [Verified on](#9-verified-on)

---

## 1. Quick start: run the packaged app

The packaged app **does not need Java installed**. It carries its own copy of Java.
It **does need a MySQL server**; see [Database setup](#2-database-setup).

1. Download `PropertyManagement-windows.zip` from the
   [latest release](https://github.com/AcherPatmos/Real-Estate-Property-Management-System/releases/tag/V1).
2. Right-click the zip and choose **Extract All**.
   Do not run the `.exe` from inside the zip: Windows copies out only the `.exe`, and it fails with
   *"Error opening ... PropertyManagement.cfg"*.
3. Open the extracted `PropertyManagement` folder, then its `app` folder, and edit `db.properties`
   with your MySQL details:

   ```properties
   db.url=jdbc:mysql://localhost:3306/propertymanagement?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=your_username_here
   db.password=your_password_here
   ```

4. Double-click `PropertyManagement.exe`.

On first launch the app creates the `propertymanagement` database and all its tables itself, then
loads a small set of demo records (see [Demo data](#6-demo-data)). If it cannot reach MySQL, it
shows a message saying so instead of opening.

---

## 2. Database setup

You need **MySQL 8** running on the same computer (or one the computer can reach).

You do **not** need to create the database or any tables. The application does that on first launch.
You only need a MySQL user that is allowed to create the `propertymanagement` database. You can use
`root`, or create a dedicated user from the MySQL command line or MySQL Workbench:

```sql
CREATE USER 'pm_user'@'localhost' IDENTIFIED BY 'choose-a-password';
GRANT ALL PRIVILEGES ON propertymanagement.* TO 'pm_user'@'localhost';
FLUSH PRIVILEGES;
```

Then put that user name and password in `db.properties`.

**Where the app looks for `db.properties`, in order:**

| Situation | File used |
|---|---|
| Packaged app (`.exe`) | `PropertyManagement/app/db.properties`, inside the app folder |
| Running from source or the JAR | `db.properties` in the folder you run the command from (the project root) |

---

## 3. Build from source

**Requirements**

- JDK 21 or newer (`java -version` should print 21 or higher).
- A running MySQL 8 server and a user as described in [Database setup](#2-database-setup).
  
**Steps**

1. Clone the repository:

   ```bash
   git clone https://github.com/AcherPatmos/Real-Estate-Property-Management-System.git
   cd Real-Estate-Property-Management-System
   ```

2. Create your settings file from the template, then open it and fill in your MySQL user and password:

   ```bash
   cp src/main/resources/db.properties.example db.properties
   ```

3. Build and run the tests with **one command**:

   ```bash
   ./mvnw clean package
   ```

   On Windows, run this in Git Bash, or use `mvnw.cmd clean package` in Command Prompt.
   The tests need MySQL running, because some of them check the database code against a real database.

4. Start the application:

   ```bash
   java -jar target/PropertyManagement-1.0-SNAPSHOT-app.jar
   ```

You can also open the project in IntelliJ IDEA and run
`com.Propertmanagement.PropertyManagerApp.PropertyManagerApp`. Put `db.properties` in the project root first.

---

## 4. Build the Windows package yourself

`package.sh` builds the self-contained app with `jpackage` (part of the JDK). Run it in **Git Bash on
Windows** from the project root:

```bash
./package.sh
```

It builds the JAR, runs the test suite, then produces:

- `target/installer/PropertyManagement/`: the app folder (`PropertyManagement.exe`, `app/`, `runtime/`)
- `target/PropertyManagement-windows.zip`: the same folder zipped, ready to hand out

jpackage builds for the operating system it runs on, so run it on Windows to get a Windows `.exe`.
If double-clicking the `.exe` seems to do nothing, build a version that opens a console window showing errors:

```bash
./package.sh --console
```

## 5. Tests and test report

The suite has **38 JUnit 5 tests** in four classes. They run automatically as part of `./mvnw clean package`.

| Test class | What it checks |
|---|---|
| `ValidationUtilsTest` (27) | Every input validation rule, including limits (exactly at a maximum length, one character past it, the largest amount a money column can hold). No database needed. |
| `MaintenanceTaskDAOTest` (3) | The recursive cost and progress calculation over task trees one, two and three levels deep. |
| `PropertyHierarchyServiceTest` (3) | Unit, occupancy, income and expense totals across a whole property. |
| `LeaseDAOTest` (5) | That a unit cannot be leased twice for overlapping dates. |

The database tests create the schema themselves before running, and they delete every record they create.

**Test report:** the report generated by the build is committed in [`docs/test-report/`](docs/test-report/).
It has one `.txt` summary and one `.xml` detailed report per test class. It was generated on Windows 11
with JDK 26.0.1, and all 38 tests passed. To regenerate it:

```bash
./mvnw clean package

```

## 6. Demo data

When the app starts and the database has **no properties**, it loads a demo data set so every screen has
something to show:

- 2 properties, 3 buildings, 5 floors and 9 units (vacant, occupied and under maintenance)
- 5 tenants with 5 leases (four active, one ended), plus rent payments
- property-level and unit-level expenses
- 3 maintenance requests, one with tasks nested three levels deep

It never runs on a database that already has data, so it cannot duplicate or overwrite your records.
To start again from the demo data, delete the database in MySQL (this **permanently erases everything in it**)
and relaunch the app:

```sql
DROP DATABASE propertymanagement;
```

---

## 7. Project structure

```
src/main/java/com/Propertmanagement/
├── PropertyManagerApp/   PropertyManagerApp: main window, sidebar navigation, start-up
├── gui/                  one screen (JPanel) per section
├── dao/                  Data Access Objects: the only classes that contain SQL
├── db/                   ConnectionManager, DbConfig, SchemaCreation, SeedData
├── factory/              MaintenanceTaskFactory: builds top-level tasks and subtasks correctly
├── service/              PropertyHierarchyService: totals across a property
├── model/                plain entity classes and result objects
└── validation/           ValidationUtils and ParsedField: input rules shared by all screens
src/test/java/            JUnit 5 tests
docs/test-report/         test report from the latest build
package.sh                builds the self-contained Windows app
```

Design notes, diagrams and the explanation of the recursive algorithm are in `project-report.pdf`.

---

## 8. Troubleshooting

| Message | Cause and fix |
|---|---|
| *"The application could not reach its database"* | MySQL is not running, or the user or password in `db.properties` is wrong. Start MySQL and check the file. |
| *"Error opening ...\PropertyManagement.cfg"* | The app was started from inside the zip, or the folder is incomplete. Use **Extract All** and run the `.exe` from the extracted folder. If it still happens, download the zip again. |
| *"The JAVA_HOME environment variable is not defined correctly"* (when building) | `JAVA_HOME` must point to the JDK folder itself, for example `C:\Program Files\Java\jdk-21`, **not** its `bin` folder. Open a new terminal after changing it. |
| *"No db.properties found"* (when building or running from source) | Copy `src/main/resources/db.properties.example` to `db.properties` in the project root and fill it in. |
| `./mvnw: Permission denied` (macOS or Linux) | Run `chmod +x mvnw` once. |

---

## 9. Verified on

The packaged application (`PropertyManagement-windows.zip`) was built and launched successfully on:

- **Machine:** Asus ExpertBook
- **Operating system:** Windows 11
- **Database:** MySQL Workbench 8.0 on the same machine

The build and test suite ran on the same machine with JDK 26.0.1.

---
