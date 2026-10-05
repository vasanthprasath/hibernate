package com.example;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StudentTest {

    private static SessionFactory sessionFactory;

    @BeforeAll
    public static void setUpClass() {
        sessionFactory = new Configuration()
                .configure("hibernate-test.cfg.xml")
                .addAnnotatedClass(Student.class)
                .buildSessionFactory();
    }

    @AfterAll
    public static void tearDownClass() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void testStudentEntityGettersAndSetters() {
        Student student = new Student();
        student.setId(1);
        student.setName("Jane Doe");
        student.setEmail("jane@example.com");
        student.setCourse("Computer Science");

        assertEquals(1, student.getId());
        assertEquals("Jane Doe", student.getName());
        assertEquals("jane@example.com", student.getEmail());
        assertEquals("Computer Science", student.getCourse());
        assertTrue(student.toString().contains("Jane Doe"));
    }

    @Test
    public void testStudentPersistenceLifecycle() {
        Student student = new Student(201, "Alice Smith", "alice@example.com", "Data Science");

        // Save student
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(student);
            session.getTransaction().commit();
        }

        // Retrieve student
        try (Session session = sessionFactory.openSession()) {
            Student retrieved = session.get(Student.class, 201);
            assertNotNull(retrieved, "Retrieved student should not be null");
            assertEquals("Alice Smith", retrieved.getName());
            assertEquals("alice@example.com", retrieved.getEmail());
            assertEquals("Data Science", retrieved.getCourse());
        }

        // Update student
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Student toUpdate = session.get(Student.class, 201);
            toUpdate.setCourse("Cyber Security");
            session.merge(toUpdate);
            session.getTransaction().commit();
        }

        // Verify update
        try (Session session = sessionFactory.openSession()) {
            Student updated = session.get(Student.class, 201);
            assertEquals("Cyber Security", updated.getCourse());
        }

        // Delete student
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            Student toDelete = session.get(Student.class, 201);
            session.remove(toDelete);
            session.getTransaction().commit();
        }

        // Verify deletion
        try (Session session = sessionFactory.openSession()) {
            Student deleted = session.get(Student.class, 201);
            assertNull(deleted, "Deleted student should be null");
        }
    }
}
