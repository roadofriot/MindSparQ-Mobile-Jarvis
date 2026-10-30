package com.jarves.mh.runtime

import com.jarves.mh.model.projectSlug
import java.io.File

enum class SupportedProjectTemplate(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val defaultLanguage: String,
) {
    GENERAL("general", "General Blank", "Custom", "Blank workspace for custom scripts and multi-language projects.", "Plain Text"),
    COMPOSE("compose", "Jetpack Compose", "Android", "Native Android app with Kotlin, Jetpack Compose, and Material 3.", "Kotlin"),
    XML("xml", "Kotlin + XML", "Android", "Traditional Native Android app with Kotlin and XML layout resources.", "Kotlin"),
    FLUTTER("flutter", "Flutter & Dart", "Cross-Platform", "Multi-platform app for Android, iOS, Desktop (Linux/Mac/Windows), and Web.", "Dart"),
    GOLANG("golang", "Go (Golang)", "Backend / CLI", "High-performance microservice REST API with Go standard net/http.", "Go"),
    PYTHON("python", "Python (FastAPI)", "Backend / AI", "FastAPI REST service with modern asynchronous endpoints.", "Python"),
    WEB("web", "Web (HTML5 / Modern JS)", "Web", "Responsive modern web application with HTML5, CSS3, and JavaScript.", "JavaScript"),
    RUST("rust", "Rust (Cargo)", "Systems / CLI", "Ultra-fast memory-safe CLI binary with Cargo.", "Rust"),
}

object MultiPlatformProjectTemplateGenerator {

    fun generate(destination: File, projectName: String, template: SupportedProjectTemplate) {
        destination.mkdirs()
        val slug = projectSlug(projectName).ifBlank { "mindsparq-app" }

        when (template) {
            SupportedProjectTemplate.FLUTTER -> generateFlutter(destination, projectName, slug)
            SupportedProjectTemplate.GOLANG -> generateGolang(destination, projectName, slug)
            SupportedProjectTemplate.PYTHON -> generatePython(destination, projectName, slug)
            SupportedProjectTemplate.WEB -> generateWeb(destination, projectName, slug)
            SupportedProjectTemplate.RUST -> generateRust(destination, projectName, slug)
            SupportedProjectTemplate.COMPOSE,
            SupportedProjectTemplate.XML,
            SupportedProjectTemplate.GENERAL -> {
                // Common readme for general / base
                File(destination, "README.md").writeText(
                    """
                    # $projectName
                    
                    Created with MindSparQ Mobile Jarvis.
                    """.trimIndent()
                )
            }
        }
    }

    private fun generateFlutter(dir: File, name: String, slug: String) {
        val cleanSlug = slug.replace('-', '_')
        File(dir, "pubspec.yaml").writeText(
            """
            name: $cleanSlug
            description: "A modern cross-platform application built with MindSparQ Jarvis."
            version: 1.0.0+1
            environment:
              sdk: ">=3.0.0 <4.0.0"
            dependencies:
              flutter:
                sdk: flutter
            dev_dependencies:
              flutter_test:
                sdk: flutter
            flutter:
              uses-material-design: true
            """.trimIndent()
        )

        val libDir = File(dir, "lib").apply { mkdirs() }
        File(libDir, "main.dart").writeText(
            """
            import 'package:flutter/material.dart';

            void main() {
              runApp(const MindSparQApp());
            }

            class MindSparQApp extends StatelessWidget {
              const MindSparQApp({super.key});

              @override
              Widget build(BuildContext context) {
                return MaterialApp(
                  title: '$name',
                  theme: ThemeData.dark().copyWith(
                    scaffoldBackgroundColor: const Color(0xFF0F141C),
                    colorScheme: ColorScheme.fromSeed(
                      seedColor: const Color(0xFF38BDF8),
                      brightness: Brightness.dark,
                    ),
                  ),
                  home: const HomePage(title: '$name'),
                );
              }
            }

            class HomePage extends StatefulWidget {
              const HomePage({super.key, required this.title});
              final String title;

              @override
              State<HomePage> createState() => _HomePageState();
            }

            class _HomePageState extends State<HomePage> {
              int _counter = 0;

              void _increment() => setState(() => _counter++);

              @override
              Widget build(BuildContext context) {
                return Scaffold(
                  appBar: AppBar(
                    title: Text(widget.title),
                    backgroundColor: const Color(0xFF161E2E),
                  ),
                  body: Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Text(
                          '🚀 Hello from MindSparQ Flutter!',
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 12),
                        Text(
                          'Interactions: ' + _counter.toString(),
                          style: const TextStyle(fontSize: 24, color: Color(0xFF38BDF8)),
                        ),
                      ],
                    ),
                  ),
                  floatingActionButton: FloatingActionButton(
                    onPressed: _increment,
                    backgroundColor: const Color(0xFF38BDF8),
                    child: const Icon(Icons.add, color: Colors.black),
                  ),
                );
              }
            }
            """.trimIndent()
        )

        File(dir, "README.md").writeText(
            """
            # $name (Flutter)

            Cross-platform application generated by MindSparQ Jarvis.
            
            ## Getting Started
            - Run `flutter run` on Android, Linux Desktop, or Chrome Web.
            """.trimIndent()
        )
    }

    private fun generateGolang(dir: File, name: String, slug: String) {
        val cleanSlug = slug.replace('-', '_')
        File(dir, "go.mod").writeText(
            """
            module $cleanSlug

            go 1.22
            """.trimIndent()
        )

        File(dir, "main.go").writeText(
            """
            package main

            import (
            	"encoding/json"
            	"fmt"
            	"log"
            	"net/http"
            	"time"
            )

            type StatusResponse struct {
            	App       string    `json:"app"`
            	Status    string    `json:"status"`
            	Timestamp time.Time `json:"timestamp"`
            }

            func main() {
            	mux := http.NewServeMux()

            	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
            		fmt.Fprintf(w, "⚡ $name is online! Built with MindSparQ Jarvis.\n")
            	})

            	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
            		w.Header().Set("Content-Type", "application/json")
            		json.NewEncoder(w).Encode(StatusResponse{
            			App:       "$name",
            			Status:    "healthy",
            			Timestamp: time.Now(),
            		})
            	})

            	addr := ":8080"
            	log.Printf("Starting $name on %s...", addr)
            	if err := http.ListenAndServe(addr, mux); err != nil {
            		log.Fatalf("Server failed: %v", err)
            	}
            }
            """.trimIndent()
        )

        File(dir, "README.md").writeText(
            """
            # $name (Golang)

            High-performance Go backend service generated by MindSparQ Jarvis.

            ## Run
            ```bash
            go run main.go
            ```
            """.trimIndent()
        )
    }

    private fun generatePython(dir: File, name: String, slug: String) {
        File(dir, "requirements.txt").writeText(
            """
            fastapi>=0.110.0
            uvicorn>=0.28.0
            pydantic>=2.6.0
            """.trimIndent()
        )

        File(dir, "main.py").writeText(
            """
            from fastapi import FastAPI
            from datetime import datetime

            app = FastAPI(title="$name", description="Generated by MindSparQ Jarvis")

            @app.get("/")
            def read_root():
                return {"message": "Hello from $name!", "author": "MindSparQ Jarvis"}

            @app.get("/health")
            def health_check():
                return {"status": "ok", "timestamp": datetime.utcnow().isoformat()}

            if __name__ == "__main__":
                import uvicorn
                uvicorn.run(app, host="0.0.0.0", port=8000)
            """.trimIndent()
        )

        File(dir, "README.md").writeText(
            """
            # $name (Python FastAPI)

            FastAPI application generated by MindSparQ Jarvis.

            ## Setup
            ```bash
            pip install -r requirements.txt
            python main.py
            ```
            """.trimIndent()
        )
    }

    private fun generateWeb(dir: File, name: String, slug: String) {
        File(dir, "index.html").writeText(
            """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>$name</title>
              <link rel="stylesheet" href="styles.css" />
            </head>
            <body>
              <div class="container">
                <header>
                  <div class="logo">⚡ MindSparQ</div>
                  <h1>$name</h1>
                  <p class="subtitle">Modern Web Application</p>
                </header>
                <main>
                  <div class="card">
                    <h2>Live Interaction</h2>
                    <p>Click below to test reactive client state:</p>
                    <button id="counter-btn" class="btn">Clicks: <span id="count">0</span></button>
                  </div>
                </main>
              </div>
              <script src="app.js"></script>
            </body>
            </html>
            """.trimIndent()
        )

        File(dir, "styles.css").writeText(
            """
            :root {
              --bg: #0f141c;
              --card: #161e2e;
              --border: #222f46;
              --primary: #38bdf8;
              --text: #f0f6fc;
              --muted: #94a3b8;
            }
            body {
              margin: 0;
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
              background-color: var(--bg);
              color: var(--text);
              display: flex;
              justify-content: center;
              padding: 40px 20px;
            }
            .container {
              max-width: 600px;
              width: 100%;
              text-align: center;
            }
            header {
              margin-bottom: 30px;
            }
            .logo {
              font-size: 14px;
              color: #f59e0b;
              font-weight: 700;
              letter-spacing: 1px;
              text-transform: uppercase;
            }
            h1 {
              margin: 8px 0;
              font-size: 32px;
            }
            .subtitle {
              color: var(--muted);
            }
            .card {
              background-color: var(--card);
              border: 1px solid var(--border);
              border-radius: 16px;
              padding: 24px;
            }
            .btn {
              background-color: var(--primary);
              color: #000;
              border: none;
              padding: 10px 20px;
              border-radius: 10px;
              font-size: 15px;
              font-weight: 600;
              cursor: pointer;
            }
            """.trimIndent()
        )

        File(dir, "app.js").writeText(
            """
            let count = 0;
            const btn = document.getElementById("counter-btn");
            const countDisplay = document.getElementById("count");

            btn.addEventListener("click", () => {
              count++;
              countDisplay.textContent = count;
            });
            """.trimIndent()
        )

        File(dir, "README.md").writeText(
            """
            # $name (Web)

            Modern HTML5/CSS/JavaScript web application generated by MindSparQ Jarvis.
            """.trimIndent()
        )
    }

    private fun generateRust(dir: File, name: String, slug: String) {
        val cleanSlug = slug.replace('-', '_')
        File(dir, "Cargo.toml").writeText(
            """
            [package]
            name = "$cleanSlug"
            version = "0.1.0"
            edition = "2021"

            [dependencies]
            """.trimIndent()
        )

        val srcDir = File(dir, "src").apply { mkdirs() }
        File(srcDir, "main.rs").writeText(
            """
            fn main() {
                println!("🚀 Hello from $name!");
                println!("Built with MindSparQ Jarvis & Rust Cargo.");
            }
            """.trimIndent()
        )

        File(dir, "README.md").writeText(
            """
            # $name (Rust)

            Systems application in Rust generated by MindSparQ Jarvis.

            ## Run
            ```bash
            cargo run
            ```
            """.trimIndent()
        )
    }
}

