package com.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static SessionFactory factory;

    public static void main(String[] args) {
        System.out.println("Initializing Hibernate SessionFactory...");

        try {
            factory = new Configuration()
                    .configure("hibernate.cfg.xml")
                    .addAnnotatedClass(Student.class)
                    .buildSessionFactory();

            // Insert initial default student if table is empty
            seedInitialData();

            int port = 8080;
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new StaticFileHandler());
            server.createContext("/api/students", new StudentApiHandler());
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("=================================================");
            System.out.println("🚀 Hibernate Student Web App Deployed Successfully!");
            System.out.println("🌐 Server listening at: http://localhost:" + port + "/");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedInitialData() {
        try (Session session = factory.openSession()) {
            session.beginTransaction();

            // Ensure the default profile is always the requested user.
            // This updates an existing ID 101 record instead of only seeding
            // Existing ID 101 is normalized to the requested profile on startup.
            Student student = session.get(Student.class, 101);
            if (student == null) {
                student = new Student(
                        101,
                        "Vasanth Prasath S",
                        "vasanthprasathsekar@gmail.com",
                        "Artificial Intelligence and Data Science"
                );
                session.persist(student);
                System.out.println("Created default student: " + student);
            } else {
                student.setName("Vasanth Prasath S");
                student.setEmail("vasanthprasathsekar@gmail.com");
                student.setCourse("Artificial Intelligence and Data Science");
                session.merge(student);
                System.out.println("Updated default student: " + student);
            }

            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Default student setup error: " + e.getMessage());
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                File file = new File("index.html");
                byte[] response;
                if (file.exists()) {
                    response = Files.readAllBytes(file.toPath());
                } else {
                    try (InputStream is = Main.class.getResourceAsStream("/index.html")) {
                        if (is != null) {
                            response = is.readAllBytes();
                        } else {
                            String fallback = "<h1>Hibernate Student App</h1><p>index.html not found</p>";
                            response = fallback.getBytes(StandardCharsets.UTF_8);
                        }
                    }
                }
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } else {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    static class StudentApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetStudents(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handlePostStudent(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteStudent(exchange);
                } else {
                    exchange.sendResponseHeaders(45, -1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                String err = "{\"error\":\"" + e.getMessage() + "\"}";
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }

        private void handleGetStudents(HttpExchange exchange) throws IOException {
            List<Student> students;
            try (Session session = factory.openSession()) {
                session.beginTransaction();
                students = session.createQuery("from Student", Student.class).list();
                session.getTransaction().commit();
            }

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                json.append(String.format(
                        "{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\",\"course\":\"%s\"}",
                        s.getId(),
                        escapeJson(s.getName()),
                        escapeJson(s.getEmail()),
                        escapeJson(s.getCourse())
                ));
                if (i < students.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void handlePostStudent(HttpExchange exchange) throws IOException {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            int id = extractInt(body, "id");
            String name = extractString(body, "name");
            String email = extractString(body, "email");
            String course = extractString(body, "course");

            Student student = new Student(id, name, email, course);

            try (Session session = factory.openSession()) {
                session.beginTransaction();
                session.merge(student);
                session.getTransaction().commit();
            }

            String resp = String.format(
                    "{\"status\":\"success\",\"student\":{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\",\"course\":\"%s\"}}",
                    id, escapeJson(name), escapeJson(email), escapeJson(course)
            );
            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(201, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void handleDeleteStudent(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            int id = -1;
            if (query != null && query.contains("id=")) {
                for (String param : query.split("&")) {
                    if (param.startsWith("id=")) {
                        id = Integer.parseInt(param.substring(3));
                    }
                }
            }

            if (id != -1) {
                try (Session session = factory.openSession()) {
                    session.beginTransaction();
                    Student s = session.get(Student.class, id);
                    if (s != null) {
                        session.remove(s);
                    }
                    session.getTransaction().commit();
                }
            }

            String resp = "{\"status\":\"deleted\",\"id\":" + id + "}";
            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String escapeJson(String input) {
            if (input == null) return "";
            return input.replace("\\", "\\\\").replace("\"", "\\\"");
        }

        private int extractInt(String json, String key) {
            Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            return (int) (System.currentTimeMillis() % 10000);
        }

        private String extractString(String json, String key) {
            Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "";
        }
    }
}
