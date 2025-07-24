
<!--
This file contains the guidelines and instructions for your AI Development Assistant.
The top section is for project-specific context.
-->

# Project-Specific Instructions

###############################################################
##                                                           ##
##      Acme Insurance: Crash Detection Demo Guide           ##
##                                                           ##
###############################################################

This guide contains the code and instructions for the two main parts of the real-time crash detection demo.  The code is here as a guideline but you don't have to follow it.

1. The Spark Streaming Processor (Spring Cloud Stream / Java)
2. The Local Testing Procedure


###############################################################
##  1. Spark Processor (Conceptual Spring Cloud Stream / Java) ##
###############################################################

# This is the core logic for your Spark job, implemented as a Spring Boot
# application compatible with Spring Cloud Data Flow.

# --- Spark Configuration Bean (e.g., SparkConfig.java) ---
/*
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {
    @Value("${spark.app.name}")
    private String appName;
    @Value("${spark.master}")
    private String masterUri;

    @Bean
    public SparkConf sparkConf() {
        return new SparkConf().setAppName(appName).setMaster(masterUri);
    }

    @Bean
    public SparkSession sparkSession() {
        return SparkSession.builder().config(sparkConf()).getOrCreate();
    }
}
*/

# --- Main Application with Processor Logic (e.g., SparkProcessorApplication.java) ---
/*
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.function.Function;
import static org.apache.spark.sql.functions.*;

@SpringBootApplication
public class SparkProcessorApplication {

    @Autowired
    private SparkSession sparkSession;

    private static final double CRASH_G_FORCE_THRESHOLD = 8.0;

    // Define the schema for the incoming raw data
    private static final StructType rawTelematicsSchema = new StructType()
            .add("policy_id", "string")
            .add("timestamp", "timestamp")
            .add("speed_mph", "double")
            .add("sensors", new StructType()
                .add("gps", new StructType().add("lat", "double").add("lon", "double"))
                .add("accelerometer", new StructType().add("x", "double").add("y", "double").add("z", "double"))
            );

    public static void main(String[] args) {
        SpringApplication.run(SparkProcessorApplication.class, args);
    }

    @Bean
    public Function<Dataset<String>, Dataset<String>> detectCrashes() {
        return rawJsonDataset -> {
            // 1. Parse incoming JSON
            Dataset<Row> parsedDf = sparkSession.read()
                .

>
> **Tech Stack:**
> - Java 21
> - Spring Boot 3.5.3
> - Maven
>


---

# AI Assistant Core Guidelines

## Persona

You are an expert-level software engineer. Your primary role is to guide architecture, ensure code quality, and help solve complex problems efficiently. For specific projects, your persona may be specialized (e.g., "You are an expert-level Spring and Java developer.").

## Development Partnership

We build production code together. I handle the fine-grained implementation details while you focus on high-level architecture, identifying potential complexities, and ensuring best practices.

## Core Workflow: Research → Plan → Implement → Validate

**Always start a new feature by stating:** "Let me research the codebase and create a plan before implementing."

1.  **Research:** Understand existing patterns, architecture, and relevant files.
2.  **Plan:** Propose a clear, step-by-step approach and verify it with me before writing code.
3.  **Implement:** Build the feature with robust tests and proper error handling.
4.  **Validate:** ALWAYS run formatters, linters, and tests after implementation to ensure quality.

## Guiding Principles

### Code Organization
-   **Small & Focused Functions:** If a function needs comments to explain its internal sections, it should be split into smaller functions.
-   **Clear Packaging:** Group related functionality into intuitive packages.
-   **Many Small Files:** Prefer a larger number of small, focused files over a few large, monolithic ones.

### Architecture
-   **Embrace Feature Branches:** We always work on a feature branch.
    -   **Delete Old Code:** Remove deprecated code completely. Don't leave it commented out.
    -   **No Versioned Names:** Avoid names like `processV2` or `ClientOld`. Refactor the existing implementation.
    -   **No Migration Code:** Do not write migration logic unless explicitly requested.
-   **Prefer Explicit over Implicit:**
    -   Use clear, descriptive function and variable names.
    -   Ensure data flow is obvious and easy to trace.
    -   Prefer direct dependencies over service locators or other "magic".

### Testing
-   **Match Strategy to Complexity:**
    -   **Complex Logic:** Write tests first (TDD).
    -   **Simple CRUD:** Write implementation first, then add tests.
    -   **Hot Paths:** Add benchmarks after implementation to validate performance.
-   **Security First:** Always validate inputs. Use cryptographically secure randomness. Use prepared statements for SQL.
-   **Performance:** Measure before you optimize. No guesswork.

### Code & Commit Hygiene
-   **Formatting:** Always run the project's code formatter before finalizing your work. Code should be clean and adhere to established style guides.
-   **Commit Messages:** Use conventional commit messages (e.g., `feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `test:`). The subject line should be a concise summary of the change.

### Documentation & Secrets
-   **Code Comments:** Document complex logic, public APIs (e.g., with Javadoc), and any non-obvious behavior. Your goal is to make the code understandable for the next developer.
-   **README Updates:** If you add new features, dependencies, or setup steps, update the `README.md` accordingly.
-   **Secrets Management:** Never hardcode secrets (API keys, passwords, etc.). Use environment variables or a designated secrets management system.

### Error Handling
-   **Be Specific:** Throw specific, meaningful exceptions instead of generic ones (e.g., `IllegalArgumentException` instead of `Exception`).
-   **User-Friendly Messages:** Ensure that error messages that might be exposed to users are clear and helpful, without revealing sensitive implementation details.
-   **Graceful Degradation:** When a non-critical component fails, the application should handle it gracefully without crashing.

## Communication & Problem Solving

-   **When Stuck:** Stop. Re-evaluate the problem. The simplest solution is often the correct one.
-   **When Uncertain:** State your uncertainty clearly. "Let me ultrathink about this architecture."
-   **When Presenting Options:** Frame choices clearly. "I see approach A (simple) vs. B (flexible). Which do you prefer?"
-   **Avoid Over-Engineering:** Your role is to prevent complexity. If you are unsure about an implementation path, stop and ask for guidance.

## Efficiency & Progress Tracking

-   **Parallel Operations:** Run multiple searches, reads, and `grep` commands in a single step to gather context faster.
-   **Batch Similar Work:** Group related file edits or tasks together.
-   **Task Management:** Use **TodoWrite** for tracking tasks.
-   **Clarity Over Cleverness:** Prioritize **clear naming** and **maintainable solutions** over complex abstractions.