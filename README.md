# MailShield AI — Intelligent Spam Email Classifier

> **A modern, privacy-focused cybersecurity web application powered by pure Java Multinomial Naive Bayes Machine Learning and Spring Boot.**

---

## 📌 Overview

**MailShield AI** is a full-stack, enterprise-grade email security dashboard that classifies email messages as **SPAM** or **HAM (Legitimate)** in real time.

Unlike mockups or wrappers around third-party paid AI APIs, MailShield AI implements the **Multinomial Naive Bayes algorithm natively in Java**. It trains directly from a labelled CSV corpus at startup, extracts text from pasted emails and raw `.eml` MIME message files, computes probabilistic Bayesian posterior confidence scores, and visualizes top threat indicators.

---

## ✨ Key Features

- **Native Java Machine Learning**: Full Multinomial Naive Bayes implementation with Laplace smoothing ($\alpha = 1.0$) and Log-Sum-Exp numerical stabilization to prevent floating-point underflow.
- **Dual Scanner Modes**:
  - **Mode A (Paste Email)**: Direct input with optional sender, optional subject, and body text.
  - **Mode B (Upload .eml File)**: Standards-compliant RFC-822 MIME parser supporting multipart messages, plain text, and HTML-only emails with attachment safety isolation.
- **Explainable AI (XAI)**: Calculates per-token Log-Likelihood Ratios ($\text{LLR}$) to display the exact suspicious or legitimate indicator tokens driving the model's verdict.
- **Persistent Scan Audit Log**: Built-in H2 database stores scan timestamps, verdicts, confidence percentages, and SHA-256 content hashes. **Raw email bodies are never persisted** to preserve privacy.
- **Real-Time Telemetry & Charts**: Interactive dashboard with real counts, threat percentages, and an SVG donut chart.
- **Rigorous Model Evaluation**: Stratified train/test split (80/20) with a 2×2 Confusion Matrix, Accuracy, Precision, Recall, and F1-score.
- **Zero API Keys Required**: 100% self-contained local execution with zero dependencies on Gmail API keys or paid cloud AI services.

---

## 📐 Machine Learning Architecture & Mathematics

The classifier implements **Multinomial Naive Bayes** with the bag-of-words assumption:

### 1. Class Prior Probability
For class $c \in \{\text{SPAM}, \text{HAM}\}$:
$$P(c) = \frac{N_c}{N_{\text{total}}}, \quad \log P(c) = \ln(N_c) - \ln(N_{\text{total}})$$

### 2. Likelihood with Laplace Smoothing
For each word $w$ in document $D$ and training vocabulary $V$:
$$P(w|c) = \frac{\text{count}(w, c) + \alpha}{\sum_{w' \in V} \text{count}(w', c) + \alpha |V|}$$
where:
- $\text{count}(w, c)$ is the occurrences of word $w$ across training emails of class $c$.
- $\alpha = 1.0$ is the Laplace smoothing parameter, preventing zero probabilities for novel words.
- $|V|$ is the size of the combined vocabulary.

### 3. Multinomial Document Score
Term frequencies $x_w = \text{count}(w, D)$ scale likelihood contributions:
$$\log P(c|D) \propto \log P(c) + \sum_{w \in D} \text{count}(w, D) \cdot \log P(w|c)$$

### 4. Posterior Normalization (Log-Sum-Exp Trick)
To prevent numerical overflow or underflow when converting log scores back to probabilities:
$$M = \max(\log P(\text{SPAM}|D), \log P(\text{HAM}|D))$$
$$P(\text{SPAM}|D) = \frac{e^{\log P(\text{SPAM}|D) - M}}{e^{\log P(\text{SPAM}|D) - M} + e^{\log P(\text{HAM}|D) - M}}$$
$$P(\text{HAM}|D) = 1.0 - P(\text{SPAM}|D)$$

### 5. Feature Explainability (Log-Likelihood Ratio)
For each token $w$:
$$\text{LLR}(w) = \log P(w|\text{SPAM}) - \log P(w|\text{HAM})$$
- $\text{LLR}(w) > 0$: Strong signal for **SPAM** (e.g., `urgent`, `verify`, `winner`, `crypto`, `wire`).
- $\text{LLR}(w) < 0$: Strong signal for **HAM** (e.g., `meeting`, `agenda`, `review`, `project`, `attached`).

---

## 🏗️ Project Architecture

```
MailShieldAI/
├── pom.xml
├── mvnw / mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/com/mailshield/
│   │   │   ├── MailShieldApplication.java
│   │   │   ├── config/
│   │   │   │   ├── ApplicationConfig.java
│   │   │   │   └── SecurityHeadersFilter.java
│   │   │   ├── controller/
│   │   │   │   ├── AnalysisController.java
│   │   │   │   ├── HistoryController.java
│   │   │   │   ├── StatisticsController.java
│   │   │   │   ├── ModelController.java
│   │   │   │   ├── ProviderController.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── dto/
│   │   │   │   ├── EmailAnalysisRequest.java
│   │   │   │   ├── EmailAnalysisResponse.java
│   │   │   │   ├── IndicatorToken.java
│   │   │   │   ├── ScanStatisticsResponse.java
│   │   │   │   └── ModelInfoResponse.java
│   │   │   ├── ml/
│   │   │   │   ├── NaiveBayesClassifier.java
│   │   │   │   ├── TextPreprocessor.java
│   │   │   │   ├── ModelEvaluator.java
│   │   │   │   ├── TrainingRecord.java
│   │   │   │   ├── EvaluationResult.java
│   │   │   │   └── ConfusionMatrix.java
│   │   │   ├── model/
│   │   │   │   ├── ClassificationRecord.java
│   │   │   │   ├── EmailData.java
│   │   │   │   └── PredictionResult.java
│   │   │   ├── repository/
│   │   │   │   └── ClassificationRepository.java
│   │   │   └── service/
│   │   │       ├── EmailAnalysisService.java
│   │   │       ├── HistoryService.java
│   │   │       ├── DatasetTrainingService.java
│   │   │       ├── EmlParserService.java
│   │   │       └── provider/
│   │   │           ├── EmailProviderService.java
│   │   │           ├── ProviderStatus.java
│   │   │           └── GmailProviderService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── dataset/
│   │       │   └── emails.csv
│   │       └── static/
│   │           ├── index.html
│   │           ├── css/styles.css
│   │           ├── js/app.js
│   │           └── images/shield-logo.svg
│   └── test/
│       └── java/com/mailshield/
│           ├── ml/
│           │   ├── NaiveBayesClassifierTest.java
│           │   ├── TextPreprocessorTest.java
│           │   └── ModelEvaluatorTest.java
│           ├── service/
│           │   ├── DatasetTrainingServiceTest.java
│           │   ├── EmlParserServiceTest.java
│           │   ├── HistoryServiceTest.java
│           │   └── EmailAnalysisServiceTest.java
│           └── controller/
│               ├── AnalysisControllerTest.java
│               └── HistoryAndStatsControllerTest.java
```

---

## 🚀 Getting Started

### Prerequisites

1. **Java Development Kit (JDK 17 or later)**:
   - Verify installation:
     ```bash
     java -version
     javac -version
     ```
   - If not installed, download from [Eclipse Temurin](https://adoptium.net/) or [Oracle OpenJDK](https://jdk.java.net/). Set your `JAVA_HOME` environment variable to the JDK installation directory.

2. **Apache Maven (3.8+)** or use the included **Maven Wrapper** (`mvnw` / `mvnw.cmd`).

---

### Running the Application

1. **Start the application with Maven**:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(Or on Windows Command Prompt / PowerShell)*:
   ```powershell
   mvn spring-boot:run
   ```

2. **Alternatively, run the packaged standalone JAR**:
   ```bash
   mvn package -DskipTests
   java -jar target/mailshield-ai-1.0.0.jar
   ```

3. **Open the Web Application**:
   Navigate to [http://localhost:8080](http://localhost:8080) in your web browser.

---

### Running Automated Tests

MailShield AI includes 39 unit and integration tests covering dataset loading, model training, Laplace smoothing, unknown words, file upload validation, history persistence, and REST APIs.

Run the test suite:
```bash
mvn test
```

Expected output:
```
[INFO] Results:
[INFO] Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🔁 Replacing or Expanding the Training Dataset

The default training dataset is located at:
```
src/main/resources/dataset/emails.csv
```

### Dataset CSV Format:
```csv
label,text
spam,"URGENT: Your bank account has been suspended! Click http://... to verify."
ham,"Hi team, please find the meeting agenda for tomorrow's sprint retrospective attached."
```

- **Header**: `label,text`
- **Labels supported**: `spam` (or `1`) and `ham` (or `legit`, `0`)
- **Quoting**: Standard RFC-4180 quotes are supported for multiline text and embedded commas.
- When you update `emails.csv` and restart the application, the `DatasetTrainingService` automatically retrains the classifier and updates all vocabulary and prior statistics.

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/analyze` | Classify pasted email text (`sender`, `subject`, `body`) |
| `POST` | `/api/analyze/upload` | Upload and parse `.eml` MIME message file |
| `GET` | `/api/history` | List recent scan records (most recent first) |
| `DELETE` | `/api/history` | Clear classification audit history |
| `GET` | `/api/statistics` | Aggregated scan metrics (total, spam, ham, ratio) |
| `GET` | `/api/model/info` | Classifier metadata, vocabulary size, class priors, and evaluation |
| `GET` | `/api/evaluation` | Held-out test set accuracy, precision, recall, F1, and confusion matrix |
| `GET` | `/api/providers` | External provider status (Gmail OAuth2 architecture) |
| `GET` | `/api/health` | Health check endpoint |

### Example API Request:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "lottery@promo-rewards.net",
    "subject": "You Won $1,000,000 Cash Prize!",
    "body": "Congratulations winner! Click http://claim.com/prize immediately to wire transfer your money."
  }'
```

### Example API Response:
```json
{
  "id": 1,
  "timestamp": "2026-10-19T05:04:01Z",
  "predictedLabel": "SPAM",
  "spamProbability": 1.0,
  "hamProbability": 0.0,
  "logPriorSpam": -0.6769,
  "logPriorHam": -0.7097,
  "totalTokens": 19,
  "recognizedTokens": 18,
  "unknownTokens": 1,
  "topSpamIndicators": [
    { "token": "your", "countInDoc": 1, "logLikelihoodRatio": 2.0946, "category": "SPAM_INDICATOR" },
    { "token": "click", "countInDoc": 1, "logLikelihoodRatio": 1.7463, "category": "SPAM_INDICATOR" },
    { "token": "__url__", "countInDoc": 1, "logLikelihoodRatio": 1.7463, "category": "SPAM_INDICATOR" }
  ],
  "topHamIndicators": [],
  "explanation": "Classified as SPAM with 100.0% confidence. Key suspicious tokens: 'your', 'immediately', 'click', '__url__'.",
  "disclaimer": "Predictions are probabilistic machine learning estimates generated by a Multinomial Naive Bayes model. A HAM classification does NOT guarantee an email is completely safe or authentic.",
  "contentHash": "175b798466a2e11dc39fd9e3ceb3e7e9e0d79c581802331680ce82e08eda4111"
}
```

---

## 🔒 Security & Privacy Guarantees

1. **Untrusted Input Treatment**: All email content is treated as potentially adversarial input.
2. **HTML Sanitization**: HTML email bodies are sanitized via **JSoup**; all scripts, stylesheets, and dangerous tags are stripped.
3. **XSS Defense**: Reflected and stored content is escaped before DOM insertion. Security headers (`Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`) are enforced globally.
4. **No Raw Body Persistence**: To prevent data leaks of confidential emails, full bodies are discarded after scoring. Only metadata and cryptographic SHA-256 digests are retained in audit tables.
5. **No Malicious Execution**: Attachments are inspected for metadata only and are never executed, unpacked into executable directories, or opened automatically.

---

## 📄 License
MailShield AI is distributed under the MIT License.
